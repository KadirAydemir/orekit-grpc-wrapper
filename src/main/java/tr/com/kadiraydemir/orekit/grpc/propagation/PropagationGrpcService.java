package tr.com.kadiraydemir.orekit.grpc.propagation;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import tr.com.kadiraydemir.orekit.mapper.PropagationMapper;
import tr.com.kadiraydemir.orekit.service.propagation.PropagationService;
import tr.com.kadiraydemir.orekit.grpc.*;

@Slf4j
@GrpcService
@RunOnVirtualThread
public class PropagationGrpcService implements OrbitalService {

    @Inject
    PropagationService propagationService;

    @Inject
    PropagationMapper propagationMapper;

    @Override
    public Uni<PropagateResponse> propagate(PropagateRequest request) {
        log.info("Propagate request received");
        return Uni.createFrom().item(() -> propagationMapper.map(propagationService.propagate(request)));
    }

    @Override
    public Multi<TLEPropagateResponse> propagateTLE(TLEPropagateRequest request) {
        log.info("TLE Propagate request received");
        return propagationService.propagateTLE(request)
                .map(propagationMapper::map);
    }
}
