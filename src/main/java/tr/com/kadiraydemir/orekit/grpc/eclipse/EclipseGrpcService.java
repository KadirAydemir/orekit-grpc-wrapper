package tr.com.kadiraydemir.orekit.grpc.eclipse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.kadiraydemir.orekit.grpc.BatchEclipseRequest;
import tr.com.kadiraydemir.orekit.grpc.EclipseRequest;
import tr.com.kadiraydemir.orekit.grpc.EclipseResponse;
import tr.com.kadiraydemir.orekit.grpc.EclipseServiceGrpc;
import tr.com.kadiraydemir.orekit.grpc.TLEPair;
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
    public void batchCalculateEclipses(BatchEclipseRequest request, StreamObserver<EclipseResponse> responseObserver) {
        List<TLEPair> allTles = request.getTlesList();
        log.info("Starting bulk eclipse calculation for {} satellites", allTles.size());

        Multi.createFrom().iterable(allTles)
                .group().intoLists().of(200) // Batch size 200
                .onItem().transformToUni(chunk -> Uni.createFrom().item(() -> processBatch(chunk, request))
                        .runSubscriptionOn(propagationExecutor))
                .merge(64) // Increased parallelism for virtual threads
                .onItem().transformToMulti(batch -> Multi.createFrom().iterable(batch))
                .merge() // Flatten stream as items finish
                .subscribe().with(
                        responseObserver::onNext,
                        responseObserver::onError,
                        responseObserver::onCompleted
                );
    }

    // Helper method to process a batch of TLEs
    private List<EclipseResponse> processBatch(List<TLEPair> chunk, BatchEclipseRequest request) {
        List<EclipseResponse> batchResponses = new ArrayList<>(chunk.size());

        for (TLEPair tlePair : chunk) {
            try {
                EclipseRequest grpcRequest = EclipseRequest.newBuilder()
                        .setTleLine1(tlePair.getLine1())
                        .setTleLine2(tlePair.getLine2())
                        .setStartDateIso(request.getStartDateIso())
                        .setEndDateIso(request.getEndDateIso())
                        .build();

                // Call eclipse service - each call returns exactly 1 EclipseResult
                EclipseResult result = eclipseService.calculateEclipses(eclipseMapper.toDTO(grpcRequest));

                if (result != null) {
                    batchResponses.add(eclipseMapper.map(result));
                }
            } catch (Exception e) {
                log.error("Error processing TLE in batch: {} - {}", tlePair.getLine1(), e.getMessage());
                // Return partial failure with error field set
                batchResponses.add(EclipseResponse.newBuilder()
                        .setNoradId(TleUtils.extractSatelliteId(tlePair.getLine1()))
                        .setError(e.getMessage())
                        .build());
            }
        }
        return batchResponses;
    }
}
