package tr.com.kadiraydemir.orekit.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import tr.com.kadiraydemir.orekit.mapper.PropagationMapper;
import tr.com.kadiraydemir.orekit.service.PropagationService;

@GrpcService
public class PropagationGrpcService implements OrbitalService {

    @Inject
    PropagationService propagationService;

    @Inject
    PropagationMapper propagationMapper;

    @Override
    public Uni<PropagateResponse> propagate(PropagateRequest request) {
        return Uni.createFrom().item(() -> propagationMapper.map(propagationService.propagate(request)));
    }

    @Override
    public Uni<TLEPropagateResponse> propagateTLE(TLEPropagateRequest request) {
        return Uni.createFrom().item(() -> propagationMapper.map(propagationService.propagateTLE(request)));
    }
}
