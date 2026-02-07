package tr.com.kadiraydemir.orekit.grpc.eclipse;

import java.util.List;
import java.util.concurrent.ExecutorService;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import tr.com.kadiraydemir.orekit.grpc.BulkEclipseRequest;
import tr.com.kadiraydemir.orekit.grpc.EclipseInterval;
import tr.com.kadiraydemir.orekit.grpc.EclipseRequest;
import tr.com.kadiraydemir.orekit.grpc.EclipseResponse;
import tr.com.kadiraydemir.orekit.grpc.EclipseServiceGrpc;
import tr.com.kadiraydemir.orekit.mapper.EclipseMapper;
import tr.com.kadiraydemir.orekit.model.EclipseResult;
import tr.com.kadiraydemir.orekit.service.eclipse.EclipseService;
import tr.com.kadiraydemir.orekit.service.eclipse.EclipseService.TLEPair;

@Slf4j
@GrpcService
@RunOnVirtualThread
public class EclipseGrpcService extends EclipseServiceGrpc.EclipseServiceImplBase {

    @Inject
    EclipseService eclipseService;

    @Inject
    EclipseMapper eclipseMapper;

    @Inject
    @Named("propagationExecutor")
    ExecutorService propagationExecutor;

    @Override
    public void calculateEclipses(EclipseRequest request, StreamObserver<EclipseResponse> responseObserver) {
        Uni.createFrom().item(() -> eclipseService.calculateEclipses(eclipseMapper.toDTO(request)))
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
                            .setNoradId(result.noradId())
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

    @Override
    public void bulkCalculateEclipses(BulkEclipseRequest request, StreamObserver<EclipseResponse> responseObserver) {
        // Convert proto TLE pairs to internal format
        List<TLEPair> tlePairs = request.getTlesList().stream()
                .map(tle -> new TLEPair(tle.getLine1(), tle.getLine2()))
                .toList();

        Multi.createFrom()
                .iterable(eclipseService.calculateEclipsesBulk(
                        tlePairs,
                        request.getStartDateIso(),
                        request.getEndDateIso()))
                .runSubscriptionOn(propagationExecutor)
                .map(this::toGrpcResponse)
                .subscribe().with(
                        responseObserver::onNext,
                        responseObserver::onError,
                        responseObserver::onCompleted
                );
    }

    private EclipseResponse toGrpcResponse(EclipseResult result) {
        var intervals = result.intervals().stream()
                .map(interval -> EclipseInterval.newBuilder()
                        .setStartIso(interval.startIso())
                        .setEndIso(interval.endIso())
                        .setDurationSeconds(interval.durationSeconds())
                        .build())
                .toList();

        return EclipseResponse.newBuilder()
                .setNoradId(result.noradId())
                .addAllIntervals(intervals)
                .build();
    }
}
