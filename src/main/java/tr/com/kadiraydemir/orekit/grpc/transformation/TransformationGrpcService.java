package tr.com.kadiraydemir.orekit.grpc.transformation;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.kadiraydemir.orekit.grpc.*;
import tr.com.kadiraydemir.orekit.mapper.TransformationMapper;
import tr.com.kadiraydemir.orekit.model.TransformResult;
import tr.com.kadiraydemir.orekit.service.transformation.TransformationService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@GrpcService
@RunOnVirtualThread
public class TransformationGrpcService extends CoordinateTransformServiceGrpc.CoordinateTransformServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(TransformationGrpcService.class);

    @Inject
    TransformationService transformationService;

    @Inject
    TransformationMapper transformationMapper;

    @Inject
    @Named("propagationExecutor")
    ExecutorService propagationExecutor;

    @Override
    public void transform(TransformRequest request, StreamObserver<TransformResponse> responseObserver) {
        Uni.createFrom().item(() -> {
                    var result = transformationService.transform(transformationMapper.toDTO(request));
                    return transformationMapper.map(result);
                })
                .runSubscriptionOn(propagationExecutor)
                .subscribe().with(
                        response -> {
                            responseObserver.onNext(response);
                            responseObserver.onCompleted();
                        },
                        responseObserver::onError
                );
    }

    @Override
    public void batchTransform(BatchTransformRequest request, StreamObserver<TransformResponse> responseObserver) {
        List<StateVector> allStates = request.getStateVectorsList();
        log.info("Starting bulk transformation for {} state vectors", allStates.size());

        Multi.createFrom().iterable(allStates)
                .group().intoLists().of(500) // Larger batch size for transformations (lighter operation)
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

    // Helper method to process a batch of state vectors
    private List<TransformResponse> processBatch(List<StateVector> chunk, BatchTransformRequest request) {
        List<TransformResponse> batchResponses = new ArrayList<>(chunk.size());

        for (StateVector stateVector : chunk) {
            try {
                TransformRequest grpcRequest = TransformRequest.newBuilder()
                        .setSourceFrame(request.getSourceFrame())
                        .setTargetFrame(request.getTargetFrame())
                        .setEpochIso(request.getEpochIso())
                        .setX(stateVector.getX())
                        .setY(stateVector.getY())
                        .setZ(stateVector.getZ())
                        .setVx(stateVector.getVx())
                        .setVy(stateVector.getVy())
                        .setVz(stateVector.getVz())
                        .build();

                // Call transformation service - each call returns exactly 1 TransformResult
                TransformResult result = transformationService.transform(transformationMapper.toDTO(grpcRequest));

                if (result != null) {
                    batchResponses.add(transformationMapper.map(result));
                }
            } catch (Exception e) {
                log.error("Error transforming state vector: ({}, {}, {}) - {}",
                        stateVector.getX(), stateVector.getY(), stateVector.getZ(), e.getMessage());
                // Return partial failure with error field set
                batchResponses.add(TransformResponse.newBuilder()
                        .setSourceFrame(request.getSourceFrame())
                        .setTargetFrame(request.getTargetFrame())
                        .setEpochIso(request.getEpochIso())
                        .setError(e.getMessage())
                        .build());
            }
        }
        return batchResponses;
    }
}
