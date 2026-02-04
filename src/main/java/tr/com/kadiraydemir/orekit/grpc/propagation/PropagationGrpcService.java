package tr.com.kadiraydemir.orekit.grpc.propagation;

import io.quarkus.grpc.GrpcService;
import io.grpc.stub.StreamObserver;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import tr.com.kadiraydemir.orekit.mapper.PropagationMapper;
import tr.com.kadiraydemir.orekit.service.propagation.PropagationService;
import tr.com.kadiraydemir.orekit.grpc.*;

@Slf4j
@GrpcService
@RunOnVirtualThread
public class PropagationGrpcService extends OrbitalServiceGrpc.OrbitalServiceImplBase {

    @Inject
    PropagationService propagationService;

    @Inject
    PropagationMapper propagationMapper;

    @Override
    public void propagate(PropagateRequest request, StreamObserver<PropagateResponse> responseObserver) {
        log.info("Propagate request received");
        try {
            var result = propagationService.propagate(request);
            responseObserver.onNext(propagationMapper.map(result));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void propagateTLE(TLEPropagateRequest request, StreamObserver<TLEPropagateResponse> responseObserver) {
        log.info("TLE Propagate request received");
        try {
            propagationService.propagateTLEBlocking(request, result -> {
                responseObserver.onNext(propagationMapper.map(result));
            });
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void propagateTLEList(TLEListPropagateRequest request, StreamObserver<TLEListPropagateResponse> responseObserver) {
        log.info("TLE List Propagate request received");
        try {
            propagationService.propagateTLEListBlocking(request, result -> {
                responseObserver.onNext(propagationMapper.map(result));
            });
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
