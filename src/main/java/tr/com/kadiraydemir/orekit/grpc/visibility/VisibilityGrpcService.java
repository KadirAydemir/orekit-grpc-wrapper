package tr.com.kadiraydemir.orekit.grpc.visibility;

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
import tr.com.kadiraydemir.orekit.mapper.VisibilityMapper;
import tr.com.kadiraydemir.orekit.model.VisibilityResult;
import tr.com.kadiraydemir.orekit.service.visibility.VisibilityService;
import tr.com.kadiraydemir.orekit.utils.TleUtils;

@GrpcService
@RunOnVirtualThread
public class VisibilityGrpcService extends VisibilityServiceGrpc.VisibilityServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(VisibilityGrpcService.class);

    @Inject
    VisibilityService visibilityService;

    @Inject
    VisibilityMapper visibilityMapper;

    @Inject
    @Named("propagationExecutor")
    ExecutorService propagationExecutor;

    @Override
    public void getAccessIntervals(AccessIntervalsRequest request, StreamObserver<AccessIntervalsResponse> responseObserver) {
        Uni.createFrom().item(() -> visibilityService.getAccessIntervals(visibilityMapper.toDTO(request)))
                .runSubscriptionOn(propagationExecutor)
                .map(visibilityMapper::map)
                .subscribe().with(
                        response -> {
                            responseObserver.onNext(response);
                            responseObserver.onCompleted();
                        },
                        responseObserver::onError
                );
    }

    @Override
    public void batchGetAccessIntervals(BatchAccessIntervalsRequest request, StreamObserver<BatchAccessIntervalsResponse> responseObserver) {
        List<TLELines> allTles = request.getTlesList();
        log.info("Starting bulk access intervals calculation for {} satellites", allTles.size());

        // Calculate date range for dynamic batch sizing
        long dateRangeDays = calculateDateRangeDays(request.getStartDateIso(), request.getEndDateIso());
        
        // Dynamic batch sizing: larger date ranges and lower elevation angles = smaller batches
        // Visibility results can have many intervals, so we use smaller base batches
        double minElevationFactor = FastMath.max(0.5, request.getMinElevationDegrees() / 10.0);
        int batchSize = (int) FastMath.min(300, FastMath.max(30, (300 / FastMath.max(1, dateRangeDays / 5)) * minElevationFactor));
        
        log.info("Dynamic batch size calculated: {} (Date range: {} days, Min elevation: {}°)", 
                batchSize, dateRangeDays, request.getMinElevationDegrees());

        Multi.createFrom().iterable(allTles)
                .onItem()
                .transformToUni(tle -> Uni.createFrom().item(() -> processSingleTle(tle, request))
                        .runSubscriptionOn(propagationExecutor))
                .merge(128) // Concurrency control
                .group().intoLists().of(batchSize) // Use dynamic batch size
                .onItem()
                .transform(results -> BatchAccessIntervalsResponse.newBuilder().addAllResults(results).build())
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
    private AccessIntervalsResponse processSingleTle(TLELines tleLines, BatchAccessIntervalsRequest request) {
        int satelliteId = TleUtils.extractSatelliteId(tleLines.getTleLine1());
        try {
            AccessIntervalsRequest grpcRequest = AccessIntervalsRequest.newBuilder()
                    .setTleLine1(tleLines.getTleLine1())
                    .setTleLine2(tleLines.getTleLine2())
                    .setGroundStation(request.getGroundStation())
                    .setStartDateIso(request.getStartDateIso())
                    .setEndDateIso(request.getEndDateIso())
                    .setMinElevationDegrees(request.getMinElevationDegrees())
                    .build();

            VisibilityResult result = visibilityService.getAccessIntervals(visibilityMapper.toDTO(grpcRequest));

            if (result != null) {
                return visibilityMapper.map(result);
            } else {
                return AccessIntervalsResponse.newBuilder()
                        .setSatelliteName(String.valueOf(satelliteId))
                        .setError("No result returned from visibility service")
                        .build();
            }
        } catch (Exception e) {
            log.error("Error processing TLE in batch: {}", tleLines.getTleLine1(), e);
            return AccessIntervalsResponse.newBuilder()
                    .setSatelliteName(String.valueOf(satelliteId))
                    .setError(e.getMessage())
                    .build();
        }
    }
}
