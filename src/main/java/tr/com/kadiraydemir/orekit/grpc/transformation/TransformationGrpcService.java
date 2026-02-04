package tr.com.kadiraydemir.orekit.grpc.transformation;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import tr.com.kadiraydemir.orekit.service.transformation.TransformationService;
import tr.com.kadiraydemir.orekit.grpc.*;

@Slf4j
@GrpcService
@RunOnVirtualThread
public class TransformationGrpcService implements CoordinateTransformService {

    @Inject
    TransformationService transformationService;

    @Override
    public Uni<TransformResponse> transform(TransformRequest request) {
        log.info("Transform request received: {} -> {}", request.getSourceFrame(), request.getTargetFrame());
        return Uni.createFrom().item(() -> transformationService.transform(request));
    }
}
