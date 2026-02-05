package tr.com.kadiraydemir.orekit.grpc.propagation;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import tr.com.kadiraydemir.orekit.grpc.*;
import tr.com.kadiraydemir.orekit.mapper.PropagationMapper;
import tr.com.kadiraydemir.orekit.service.propagation.PropagationService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SubmissionPublisher;

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
        Uni.createFrom().item(() -> propagationService.propagate(propagationMapper.toDTO(request)))
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
        propagationService.propagateTLE(propagationMapper.toDTO(request))
                .subscribe().with(
                        result -> responseObserver.onNext(propagationMapper.map(result)),
                        responseObserver::onError,
                        responseObserver::onCompleted
                );
    }

    @Override
    public StreamObserver<TLEPropagateRequest> propagateTLEStream(StreamObserver<TLEPropagateResponse> responseObserver) {
        SubmissionPublisher<TLEPropagateRequest> publisher = new SubmissionPublisher<>();

        Multi.createFrom().publisher(publisher)
                .flatMap(request -> propagationService.propagateTLE(propagationMapper.toDTO(request))
                        .map(propagationMapper::map)
                )
                .subscribe().with(
                        responseObserver::onNext,
                        responseObserver::onError,
                        responseObserver::onCompleted
                );

        return new StreamObserver<TLEPropagateRequest>() {
            @Override
            public void onNext(TLEPropagateRequest value) {
                publisher.submit(value);
            }

            @Override
            public void onError(Throwable t) {
                publisher.closeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                publisher.close();
            }
        };
    }
}
