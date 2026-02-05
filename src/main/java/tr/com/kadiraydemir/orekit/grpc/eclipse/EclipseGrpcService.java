package tr.com.kadiraydemir.orekit.grpc.eclipse;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import tr.com.kadiraydemir.orekit.grpc.EclipseInterval;
import tr.com.kadiraydemir.orekit.grpc.EclipseRequest;
import tr.com.kadiraydemir.orekit.grpc.EclipseResponse;
import tr.com.kadiraydemir.orekit.grpc.EclipseServiceGrpc;
import tr.com.kadiraydemir.orekit.service.eclipse.EclipseService;

import java.util.concurrent.ExecutorService;

@Slf4j
@GrpcService
@RunOnVirtualThread
public class EclipseGrpcService extends EclipseServiceGrpc.EclipseServiceImplBase {

    @Inject
    EclipseService eclipseService;

    @Inject
    @Named("propagationExecutor")
    ExecutorService propagationExecutor;

    @Override
    public void calculateEclipses(EclipseRequest request, StreamObserver<EclipseResponse> responseObserver) {
        Uni.createFrom().item(() -> eclipseService.calculateEclipses(request))
                .runSubscriptionOn(propagationExecutor)
                .map(result -> {
                    var intervals = result.intervals().stream()
                            .map(interval -> EclipseInterval.newBuilder()
                                    .setStartIso(interval.startIso())
                                    .setEndIso(interval.endIso())
                                    .setDurationSeconds(interval.durationSeconds())
                                    .build())
                            .toList();

                    return EclipseResponse.newBuilder()
                            .setSatelliteName(result.satelliteName())
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
