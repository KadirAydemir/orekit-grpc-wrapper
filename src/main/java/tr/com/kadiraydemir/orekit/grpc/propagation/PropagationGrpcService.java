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
        public void batchPropagateTLE(BatchTLEPropagateRequest request,
                        StreamObserver<BatchTLEPropagateResponse> responseObserver) {
                List<TLELines> allTles = request.getTlesList();
                log.info("Starting bulk TLE propagation for {} satellites", allTles.size());

                Multi.createFrom().iterable(allTles)
                                .onItem()
                                .transformToUni(tle -> Uni.createFrom().item(() -> processSingleTle(tle, request))
                                                .runSubscriptionOn(propagationExecutor))
                                .merge(128) // Concurrency control
                                .group().intoLists().of(100) // Batch responses in groups of 100
                                .onItem()
                                .transform(results -> BatchTLEPropagateResponse.newBuilder().addAllResults(results)
                                                .build())
                                .subscribe().with(
                                                responseObserver::onNext,
                                                responseObserver::onError,
                                                responseObserver::onCompleted);
        }

        // Helper method to process a single TLE with error handling
        private TLEPropagationResult processSingleTle(TLELines tleLines, BatchTLEPropagateRequest request) {
                int satelliteId = TleUtils.extractSatelliteId(tleLines.getTleLine1());
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

                        TleResult result = propagationService.propagateTLE(propagationMapper.toDTO(grpcRequest))
                                        .toUni().await().indefinitely();

                        if (result != null) {
                                return TLEPropagationResult.newBuilder()
                                                .setSatelliteId(satelliteId)
                                                .addAllPositions(result.positions().stream()
                                                                .map(p -> PositionPoint.newBuilder()
                                                                                .setX(p.x())
                                                                                .setY(p.y())
                                                                                .setZ(p.z())
                                                                                .setTimestamp(p.timestamp())
                                                                                .build())
                                                                .toList())
                                                .setFrame(result.frame())
                                                .build();
                        } else {
                                return TLEPropagationResult.newBuilder()
                                                .setSatelliteId(satelliteId)
                                                .setError("No result returned from propagation service")
                                                .build();
                        }
                } catch (Exception e) {
                        log.error("Error processing TLE in batch: {}", tleLines.getTleLine1(), e);
                        return TLEPropagationResult.newBuilder()
                                        .setSatelliteId(satelliteId)
                                        .setError(e.getMessage())
                                        .build();
                }
        }
}
