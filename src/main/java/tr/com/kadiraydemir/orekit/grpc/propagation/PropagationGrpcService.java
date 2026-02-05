package tr.com.kadiraydemir.orekit.grpc.propagation;

import io.quarkus.grpc.GrpcService;
import io.grpc.stub.StreamObserver;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import tr.com.kadiraydemir.orekit.mapper.PropagationMapper;
import tr.com.kadiraydemir.orekit.service.propagation.PropagationService;
import tr.com.kadiraydemir.orekit.grpc.*;

import java.util.concurrent.ExecutorService;

@Slf4j
@GrpcService
@RunOnVirtualThread
public class PropagationGrpcService extends OrbitalServiceGrpc.OrbitalServiceImplBase {

    @Inject
    PropagationService propagationService;

    @Inject
    PropagationMapper propagationMapper;

    @Inject
    @Named("propagationExecutor")
    ExecutorService propagationExecutor;

    @Override
    public void propagate(PropagateRequest request, StreamObserver<PropagateResponse> responseObserver) {
        Uni.createFrom().item(() -> propagationService.propagate(request))
                .runSubscriptionOn(propagationExecutor)
                .map(propagationMapper::map)
                .subscribe().with(
                        response -> {
                            responseObserver.onNext(response);
                            responseObserver.onCompleted();
                        },
                        responseObserver::onError
                );
    }

    @Override
    public void propagateTLE(TLEPropagateRequest request, StreamObserver<TLEPropagateResponse> responseObserver) {
        propagationService.propagateTLE(request)
                .subscribe().with(
                        result -> responseObserver.onNext(propagationMapper.map(result)),
                        responseObserver::onError,
                        responseObserver::onCompleted
                );
    }
}
