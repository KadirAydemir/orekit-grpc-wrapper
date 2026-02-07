package tr.com.kadiraydemir.orekit.grpc.visibility;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.kadiraydemir.orekit.grpc.*;
import tr.com.kadiraydemir.orekit.mapper.VisibilityMapper;
import tr.com.kadiraydemir.orekit.model.VisibilityResult;
import tr.com.kadiraydemir.orekit.service.visibility.VisibilityService;
import tr.com.kadiraydemir.orekit.utils.TleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

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
    public void batchGetAccessIntervals(BatchAccessIntervalsRequest request, StreamObserver<AccessIntervalsResponse> responseObserver) {
        List<TLELines> allTles = request.getTlesList();
        log.info("Starting bulk access intervals calculation for {} satellites", allTles.size());

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
    private List<AccessIntervalsResponse> processBatch(List<TLELines> chunk, BatchAccessIntervalsRequest request) {
        List<AccessIntervalsResponse> batchResponses = new ArrayList<>(chunk.size());

        for (TLELines tleLines : chunk) {
            try {
                AccessIntervalsRequest grpcRequest = AccessIntervalsRequest.newBuilder()
                        .setTleLine1(tleLines.getTleLine1())
                        .setTleLine2(tleLines.getTleLine2())
                        .setGroundStation(request.getGroundStation())
                        .setStartDateIso(request.getStartDateIso())
                        .setEndDateIso(request.getEndDateIso())
                        .setMinElevationDegrees(request.getMinElevationDegrees())
                        .build();

                // Call visibility service - each call returns exactly 1 VisibilityResult
                VisibilityResult result = visibilityService.getAccessIntervals(visibilityMapper.toDTO(grpcRequest));

                if (result != null) {
                    batchResponses.add(visibilityMapper.map(result));
                }
            } catch (Exception e) {
                log.error("Error processing TLE in batch: {} - {}", tleLines.getTleLine1(), e.getMessage());
                // Return partial failure with error field set
                batchResponses.add(AccessIntervalsResponse.newBuilder()
                        .setSatelliteName(String.valueOf(TleUtils.extractSatelliteId(tleLines.getTleLine1())))
                        .setError(e.getMessage())
                        .build());
            }
        }
        return batchResponses;
    }
}
