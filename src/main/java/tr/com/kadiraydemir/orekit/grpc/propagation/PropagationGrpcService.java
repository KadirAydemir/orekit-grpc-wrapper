package tr.com.kadiraydemir.orekit.grpc.propagation;

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
import tr.com.kadiraydemir.orekit.mapper.PropagationMapper;
import tr.com.kadiraydemir.orekit.model.TleResult;
import tr.com.kadiraydemir.orekit.service.propagation.PropagationService;
import tr.com.kadiraydemir.orekit.utils.TleUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@GrpcService
@RunOnVirtualThread
public class PropagationGrpcService extends OrbitalServiceGrpc.OrbitalServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(PropagationGrpcService.class);

    @Inject
    PropagationService propagationService;

    @Inject
    PropagationMapper propagationMapper;

    @Inject
    @Named("propagationExecutor")
    ExecutorService propagationExecutor;

    @Override
    public void propagate(PropagateRequest request, StreamObserver<PropagateResponse> responseObserver) {
        Uni.createFrom().item(() -> propagationService.propagate(propagationMapper.toDTO(request)))
                .runSubscriptionOn(propagationExecutor)
                .map(propagationMapper::map)
                .subscribe().with(
                        response -> {
                            responseObserver.onNext(response);
                            responseObserver.onCompleted();
                        },
                        responseObserver::onError);
    }

    @Override
    public void propagateTLE(tr.com.kadiraydemir.orekit.grpc.TLEPropagateRequest request,
            StreamObserver<TLEPropagateResponse> responseObserver) {
        propagationService.propagateTLE(propagationMapper.toDTO(request))
                .subscribe().with(
                        result -> responseObserver.onNext(propagationMapper.map(result)),
                        responseObserver::onError,
                        responseObserver::onCompleted);
    }

    @Override
    public void batchPropagateTLE(BatchTLEPropagateRequest request, StreamObserver<BatchTLEPropagateResponse> responseObserver) {
        List<TLELines> allTles = request.getTlesList();
        log.info("Starting bulk TLE propagation for {} satellites", allTles.size());

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
                        responseObserver::onCompleted);
    }

    // Helper method to process a batch of TLEs
    private List<BatchTLEPropagateResponse> processBatch(List<TLELines> chunk, BatchTLEPropagateRequest request) {
        List<BatchTLEPropagateResponse> batchResponses = new ArrayList<>(chunk.size());

        for (TLELines tleLines : chunk) {
            try {
                tr.com.kadiraydemir.orekit.grpc.TLEPropagateRequest grpcRequest = tr.com.kadiraydemir.orekit.grpc.TLEPropagateRequest
                        .newBuilder()
                        .setModel(request.getModel())
                        .setTleLine1(tleLines.getTleLine1())
                        .setTleLine2(tleLines.getTleLine2())
                        .setStartDate(request.getStartDate())
                        .setEndDate(request.getEndDate())
                        .setPositionCount(request.getPositionCount())
                        .setOutputFrame(request.getOutputFrame())
                        .setIntegrator(request.getIntegrator())
                        .build();

                // Call propagation service - each call returns exactly 1 TleResult for TLE
                // model
                TleResult result = propagationService.propagateTLE(propagationMapper.toDTO(grpcRequest))
                        .toUni().await().indefinitely();

                if (result != null) {
                    batchResponses.add(BatchTLEPropagateResponse.newBuilder()
                            .setSatelliteId(TleUtils.extractSatelliteId(tleLines.getTleLine1()))
                            .addAllPositions(result.positions().stream()
                                    .map(p -> PositionPoint.newBuilder()
                                            .setX(p.x())
                                            .setY(p.y())
                                            .setZ(p.z())
                                            .setTimestamp(p.timestamp())
                                            .build())
                                    .toList())
                            .setFrame(result.frame())
                            .build());
                }
            } catch (Exception e) {
                log.error("Error processing TLE in batch: {}", tleLines.getTleLine1(), e);
                batchResponses.add(BatchTLEPropagateResponse.newBuilder()
                        .setSatelliteId(TleUtils.extractSatelliteId(tleLines.getTleLine1()))
                        .setError(e.getMessage())
                        .build());
            }
        }
        return batchResponses;
    }
}
