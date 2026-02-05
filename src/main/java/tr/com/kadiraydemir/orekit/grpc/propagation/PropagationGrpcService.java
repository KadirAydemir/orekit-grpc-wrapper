package tr.com.kadiraydemir.orekit.grpc.propagation;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import tr.com.kadiraydemir.orekit.grpc.*;
import tr.com.kadiraydemir.orekit.mapper.PropagationMapper;
import tr.com.kadiraydemir.orekit.service.propagation.PropagationService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SubmissionPublisher;

@Slf4j
@GrpcService
@RunOnVirtualThread
public class PropagationGrpcService extends OrbitalServiceGrpc.OrbitalServiceImplBase {

    @Inject
    PropagationService propagationService;

    @Inject
    PropagationMapper propagationMapper;

    @Inject
    @Named("propagationExecutor")
    ExecutorService propagationExecutor;

    @Override
    public void propagate(PropagateRequest request, StreamObserver<PropagateResponse> responseObserver) {
        Uni.createFrom().item(() -> propagationService.propagate(propagationMapper.toDTO(request)))
                .runSubscriptionOn(propagationExecutor)
                .map(propagationMapper::map)
                .subscribe().with(
                        response -> {
                            responseObserver.onNext(response);
                            responseObserver.onCompleted();
                        },
                        responseObserver::onError);
    }

    @Override
    public void propagateTLE(TLEPropagateRequest request, StreamObserver<TLEPropagateResponse> responseObserver) {
        propagationService.propagateTLE(propagationMapper.toDTO(request))
                .subscribe().with(
                        result -> responseObserver.onNext(propagationMapper.map(result)),
                        responseObserver::onError,
                        responseObserver::onCompleted);
    }

    @Override
    public StreamObserver<TLEStreamRequest> propagateTLEStream(StreamObserver<TLEStreamResponse> responseObserver) {
        final TLEStreamConfig[] configHolder = new TLEStreamConfig[1];
        // Buffer size increased to 32768 (2^15) to handle large bursts (approx 30k
        // satellites) without blocking
        SubmissionPublisher<TLELines> publisher = new SubmissionPublisher<>(propagationExecutor, 32768);

        Multi.createFrom().publisher(publisher)
                .onItem().transformToMulti(tleLines -> {
                    if (configHolder[0] == null) {
                        return Multi.createFrom().<TLEStreamResponse>failure(
                                new IllegalStateException("Config must be sent before TLEs"));
                    }
                    try {
                        TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                                .setModel(configHolder[0].getModel())
                                .setTleLine1(tleLines.getTleLine1())
                                .setTleLine2(tleLines.getTleLine2())
                                .setStartDate(configHolder[0].getStartDate())
                                .setEndDate(configHolder[0].getEndDate())
                                .setPositionCount(configHolder[0].getPositionCount())
                                .setOutputFrame(configHolder[0].getOutputFrame())
                                .setIntegrator(configHolder[0].getIntegrator())
                                .build();

                        return propagationService.propagateTLE(propagationMapper.toDTO(request))
                                .map(result -> TLEStreamResponse.newBuilder()
                                        .setSatelliteId(extractSatelliteId(tleLines.getTleLine1()))
                                        .addAllPositions(result.positions().stream()
                                                .map(p -> PositionPoint.newBuilder()
                                                        .setX(p.x())
                                                        .setY(p.y())
                                                        .setZ(p.z())
                                                        .setTimestamp(p.timestamp())
                                                        .build())
                                                .toList())
                                        .setFrame(result.frame())
                                        .build());
                    } catch (Exception e) {
                        log.error("Error processing TLE: {}", tleLines.getTleLine1(), e);
                        return Multi.createFrom().item(TLEStreamResponse.newBuilder()
                                .setSatelliteId(extractSatelliteId(tleLines.getTleLine1()))
                                .setError(e.getMessage())
                                .build());
                    }
                })
                .merge(128)
                .subscribe().with(
                        responseObserver::onNext,
                        responseObserver::onError,
                        responseObserver::onCompleted);

        return new StreamObserver<TLEStreamRequest>() {
            @Override
            public void onNext(TLEStreamRequest value) {
                try {
                    if (value.hasConfig()) {
                        configHolder[0] = value.getConfig();
                    } else if (value.hasTleList()) {
                        if (configHolder[0] == null) {
                            log.error("TLE received before config");
                            responseObserver.onError(new IllegalStateException("Config must be sent before TLEs"));
                            return;
                        }
                        // Submit each TLE in the chunk to the internal publisher
                        value.getTleList().getLinesList().forEach(publisher::submit);
                    }
                } catch (Exception e) {
                    log.error("Error in onNext: {}", e.getMessage(), e);
                    responseObserver.onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Stream error: {}", t.getMessage(), t);
                publisher.closeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                publisher.close();
            }
        };
    }

    private int extractSatelliteId(String line1) {
        try {
            return Integer.parseInt(line1.substring(2, 7).trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
