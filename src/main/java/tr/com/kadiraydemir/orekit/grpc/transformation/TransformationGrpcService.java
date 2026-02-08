package tr.com.kadiraydemir.orekit.grpc.transformation;

import java.util.List;
import java.util.concurrent.ExecutorService;

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
    public void batchTransform(BatchTransformRequest request, StreamObserver<BatchTransformResponse> responseObserver) {
        List<StateVector> allStates = request.getStateVectorsList();
        log.info("Starting bulk transformation for {} state vectors", allStates.size());

        // Dynamic batch sizing for coordinate transforms
        // Transform results are very small (just 6 doubles + metadata), so we can use large batches
        // Target: 1000 results per batch for optimal network efficiency
        int batchSize = 1000;
        
        log.info("Dynamic batch size calculated: {} (Coordinate transforms are lightweight)", batchSize);

        Multi.createFrom().iterable(allStates)
                .onItem()
                .transformToUni(stateVector -> Uni.createFrom().item(() -> processSingleStateVector(stateVector, request))
                        .runSubscriptionOn(propagationExecutor))
                .merge(128) // Concurrency control
                .group().intoLists().of(batchSize) // Use dynamic batch size
                .onItem()
                .transform(results -> BatchTransformResponse.newBuilder().addAllResults(results).build())
                .subscribe().with(
                        responseObserver::onNext,
                        responseObserver::onError,
                        responseObserver::onCompleted);
    }

    // Helper method to process a single state vector with error handling
    private TransformResponse processSingleStateVector(StateVector stateVector, BatchTransformRequest request) {
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

            TransformResult result = transformationService.transform(transformationMapper.toDTO(grpcRequest));

            if (result != null) {
                return transformationMapper.map(result);
            } else {
                return TransformResponse.newBuilder()
                        .setSourceFrame(request.getSourceFrame())
                        .setTargetFrame(request.getTargetFrame())
                        .setEpochIso(request.getEpochIso())
                        .setError("No result returned from transformation service")
                        .build();
            }
        } catch (Exception e) {
            log.error("Error transforming state vector: ({}, {}, {}) - {}",
                    stateVector.getX(), stateVector.getY(), stateVector.getZ(), e.getMessage());
            return TransformResponse.newBuilder()
                    .setSourceFrame(request.getSourceFrame())
                    .setTargetFrame(request.getTargetFrame())
                    .setEpochIso(request.getEpochIso())
                    .setError(e.getMessage())
                    .build();
        }
    }
}
