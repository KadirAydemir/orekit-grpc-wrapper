package tr.com.kadiraydemir.orekit.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import tr.com.kadiraydemir.orekit.mapper.PropagationMapper;
import tr.com.kadiraydemir.orekit.service.PropagationService;

@GrpcService
@RunOnVirtualThread
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
    public Multi<TLEPropagateResponse> propagateTLE(TLEPropagateRequest request) {
        return propagationService.propagateTLE(request)
                .map(propagationMapper::map);
    }
}
