package tr.com.kadiraydemir.orekit.grpc.transformation;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import tr.com.kadiraydemir.orekit.mapper.TransformationMapper;
import tr.com.kadiraydemir.orekit.service.transformation.TransformationService;
import tr.com.kadiraydemir.orekit.grpc.*;

import java.util.concurrent.ExecutorService;

@Slf4j
@GrpcService
@RunOnVirtualThread
public class TransformationGrpcService implements CoordinateTransformService {

    @Inject
    TransformationService transformationService;

    @Inject
    TransformationMapper transformationMapper;

    @Inject
    @Named("propagationExecutor")
    ExecutorService propagationExecutor;

    @Override
    public Uni<TransformResponse> transform(TransformRequest request) {
        return Uni.createFrom().item(() -> {
            var result = transformationService.transform(transformationMapper.toDTO(request));
            return transformationMapper.map(result);
        })
        .runSubscriptionOn(propagationExecutor);
    }
}
