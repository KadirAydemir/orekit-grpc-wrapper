package tr.com.kadiraydemir.orekit.grpc.visibility;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import tr.com.kadiraydemir.orekit.grpc.*;

@Slf4j
@GrpcService
@RunOnVirtualThread
public class VisibilityGrpcService implements VisibilityService {

    @Inject
    tr.com.kadiraydemir.orekit.service.visibility.VisibilityService visibilityService;

    @Override
    public Uni<AccessIntervalsResponse> getAccessIntervals(AccessIntervalsRequest request) {
        log.info("Visibility request received for station: {}", request.getGroundStation().getName());
        return Uni.createFrom().item(() -> visibilityService.getAccessIntervals(request));
    }
}
