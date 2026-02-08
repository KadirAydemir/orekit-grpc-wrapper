package tr.com.kadiraydemir.orekit.grpc.eclipse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.hipparchus.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.kadiraydemir.orekit.grpc.*;
import tr.com.kadiraydemir.orekit.mapper.EclipseMapper;
import tr.com.kadiraydemir.orekit.model.EclipseResult;
import tr.com.kadiraydemir.orekit.service.eclipse.EclipseService;
import tr.com.kadiraydemir.orekit.utils.TleUtils;

@GrpcService
@RunOnVirtualThread
public class EclipseGrpcService extends EclipseServiceGrpc.EclipseServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(EclipseGrpcService.class);

    @Inject
    EclipseService eclipseService;

    @Inject
    EclipseMapper eclipseMapper;

    @Inject
    @Named("propagationExecutor")
    ExecutorService propagationExecutor;

    @Override
    public void calculateEclipses(EclipseRequest request, StreamObserver<EclipseResponse> responseObserver) {
        Uni.createFrom().item(() -> eclipseService.calculateEclipses(eclipseMapper.toDTO(request)))
                .runSubscriptionOn(propagationExecutor)
                .map(eclipseMapper::map)
                .subscribe().with(
                        response -> {
                            responseObserver.onNext(response);
                            responseObserver.onCompleted();
                        },
                        responseObserver::onError
                );
    }

    @Override
    public void batchCalculateEclipses(BatchEclipseRequest request, StreamObserver<BatchEclipseResponse> responseObserver) {
        List<TLEPair> allTles = request.getTlesList();
        log.info("Starting bulk eclipse calculation for {} satellites", allTles.size());

        // Calculate date range for dynamic batch sizing
        long dateRangeDays = calculateDateRangeDays(request.getStartDateIso(), request.getEndDateIso());
        
        // Dynamic batch sizing: larger date ranges = smaller batches
        // Eclipse results are small (just intervals), so we can use larger batches
        // Base batch size: 500, reduce for longer durations
        int batchSize = (int) FastMath.min(500, FastMath.max(50, 500 / FastMath.max(1, dateRangeDays / 7)));
        
        log.info("Dynamic batch size calculated: {} (Date range: {} days)", batchSize, dateRangeDays);

        Multi.createFrom().iterable(allTles)
                .onItem()
                .transformToUni(tle -> Uni.createFrom().item(() -> processSingleTle(tle, request))
                        .runSubscriptionOn(propagationExecutor))
                .merge(128) // Concurrency control
                .group().intoLists().of(batchSize) // Use dynamic batch size
                .onItem()
                .transform(results -> BatchEclipseResponse.newBuilder().addAllResults(results).build())
                .subscribe().with(
                        responseObserver::onNext,
                        responseObserver::onError,
                        responseObserver::onCompleted);
    }

    // Helper method to calculate date range in days
    private long calculateDateRangeDays(String startDateIso, String endDateIso) {
        try {
            Instant start = Instant.parse(startDateIso);
            Instant end = Instant.parse(endDateIso);
            return Duration.between(start, end).toDays();
        } catch (Exception e) {
            log.warn("Could not parse date range, using default batch size");
            return 1;
        }
    }

    // Helper method to process a single TLE with error handling
    private EclipseResponse processSingleTle(TLEPair tlePair, BatchEclipseRequest request) {
        int satelliteId = TleUtils.extractSatelliteId(tlePair.getLine1());
        try {
            EclipseRequest grpcRequest = EclipseRequest.newBuilder()
                    .setTleLine1(tlePair.getLine1())
                    .setTleLine2(tlePair.getLine2())
                    .setStartDateIso(request.getStartDateIso())
                    .setEndDateIso(request.getEndDateIso())
                    .build();

            EclipseResult result = eclipseService.calculateEclipses(eclipseMapper.toDTO(grpcRequest));

            if (result != null) {
                return eclipseMapper.map(result);
            } else {
                return EclipseResponse.newBuilder()
                        .setNoradId(satelliteId)
                        .setError("No result returned from eclipse service")
                        .build();
            }
        } catch (Exception e) {
            log.error("Error processing TLE in batch: {}", tlePair.getLine1(), e);
            return EclipseResponse.newBuilder()
                    .setNoradId(satelliteId)
                    .setError(e.getMessage())
                    .build();
        }
    }
}
