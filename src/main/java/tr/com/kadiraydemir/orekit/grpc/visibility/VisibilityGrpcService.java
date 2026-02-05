package tr.com.kadiraydemir.orekit.grpc.visibility;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import tr.com.kadiraydemir.orekit.mapper.VisibilityMapper;
import tr.com.kadiraydemir.orekit.grpc.*;

import java.util.concurrent.ExecutorService;

@Slf4j
@GrpcService
@RunOnVirtualThread
public class VisibilityGrpcService extends VisibilityServiceGrpc.VisibilityServiceImplBase {

    @Inject
    tr.com.kadiraydemir.orekit.service.visibility.VisibilityService visibilityService;

    @Inject
    VisibilityMapper visibilityMapper;

    @Inject
    @Named("propagationExecutor")
    ExecutorService propagationExecutor;

    @Override
    public void getAccessIntervals(AccessIntervalsRequest request, StreamObserver<AccessIntervalsResponse> responseObserver) {
        Uni.createFrom().item(() -> visibilityService.getAccessIntervals(visibilityMapper.toDTO(request)))
                .runSubscriptionOn(propagationExecutor)
                .map(result -> {
                    var intervals = result.intervals().stream()
                            .map(interval -> AccessInterval.newBuilder()
                                    .setStartIso(interval.startIso())
                                    .setEndIso(interval.endIso())
                                    .setDurationSeconds(interval.durationSeconds())
                                    .build())
                            .toList();

                    return AccessIntervalsResponse.newBuilder()
                            .setSatelliteName(result.satelliteName())
                            .setStationName(result.stationName())
                            .addAllIntervals(intervals)
                            .build();
                })
                .subscribe().with(
                        response -> {
                            responseObserver.onNext(response);
                            responseObserver.onCompleted();
                        },
                        responseObserver::onError
                );
    }
}
