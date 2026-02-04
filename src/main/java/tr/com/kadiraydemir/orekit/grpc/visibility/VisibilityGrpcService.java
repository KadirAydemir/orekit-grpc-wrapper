package tr.com.kadiraydemir.orekit.grpc.visibility;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import tr.com.kadiraydemir.orekit.grpc.*;

@Slf4j
@GrpcService
@RunOnVirtualThread
public class VisibilityGrpcService extends VisibilityServiceGrpc.VisibilityServiceImplBase {

    @Inject
    tr.com.kadiraydemir.orekit.service.visibility.VisibilityService visibilityService;

    @Override
    public void getAccessIntervals(AccessIntervalsRequest request, StreamObserver<AccessIntervalsResponse> responseObserver) {
        log.info("Visibility request received for station: {}", request.getGroundStation().getName());
        try {
            var result = visibilityService.getAccessIntervals(request);
            var intervals = result.intervals().stream()
                    .map(interval -> AccessInterval.newBuilder()
                            .setStartIso(interval.startIso())
                            .setEndIso(interval.endIso())
                            .setDurationSeconds(interval.durationSeconds())
                            .build())
                    .toList();

            AccessIntervalsResponse response = AccessIntervalsResponse.newBuilder()
                    .setSatelliteName(result.satelliteName())
                    .setStationName(result.stationName())
                    .addAllIntervals(intervals)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing visibility request", e);
            responseObserver.onError(e);
        }
    }
}
