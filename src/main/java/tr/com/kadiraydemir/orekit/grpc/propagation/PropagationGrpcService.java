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
import tr.com.kadiraydemir.orekit.model.TleResult;
import tr.com.kadiraydemir.orekit.service.propagation.PropagationService;

import java.util.ArrayList;
import java.util.List;
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
    public void propagateTLE(tr.com.kadiraydemir.orekit.grpc.TLEPropagateRequest request,
            StreamObserver<TLEPropagateResponse> responseObserver) {
        propagationService.propagateTLE(propagationMapper.toDTO(request))
                .subscribe().with(
                        result -> responseObserver.onNext(propagationMapper.map(result)),
                        responseObserver::onError,
                        responseObserver::onCompleted);
    }

    @Override
    public void propagateTLEList(TLEListRequest request, StreamObserver<TLEStreamResponse> responseObserver) {
        // Validation
        if (!request.hasConfig()) {
            responseObserver.onError(new IllegalArgumentException("Config is required"));
            return;
        }

        TLEStreamConfig config = request.getConfig();
        List<TLELines> allTles = request.getTlesList();

        // Chunk processing logic
        // We use Multi to process chunks in parallel on the executor, and stream
        // results immediately
        Multi.createFrom().iterable(allTles)
                .group().intoLists().of(200) // Batch size 200
                .onItem().transformToUni(chunk -> Uni.createFrom().item(() -> processBatch(chunk, config))
                        .runSubscriptionOn(propagationExecutor))
                .merge(32) // Parallelism level (up to 32 chunks in flight)
                .onItem().transformToMulti(batchResults -> Multi.createFrom().iterable(batchResults)) // Flatten batch
                                                                                                      // results into
                                                                                                      // stream
                .concatenate() // Flatten the stream (Multi<TLEStreamResponse>)
                .subscribe().with(
                        responseObserver::onNext, // Send each TLEStreamResponse immediately
                        responseObserver::onError,
                        responseObserver::onCompleted);
    }

    // Helper method to process a batch of TLEs
    private List<TLEStreamResponse> processBatch(List<TLELines> chunk, TLEStreamConfig config) {
        List<TLEStreamResponse> batchResponses = new ArrayList<>(chunk.size());

        for (TLELines tleLines : chunk) {
            try {
                // Manually map to internal DTO (since we don't have a direct mapper for
                // TLELines -> Request)
                // Reusing the same request builder logic

                tr.com.kadiraydemir.orekit.grpc.TLEPropagateRequest grpcRequest = tr.com.kadiraydemir.orekit.grpc.TLEPropagateRequest
                        .newBuilder()
                        .setModel(config.getModel())
                        .setTleLine1(tleLines.getTleLine1())
                        .setTleLine2(tleLines.getTleLine2())
                        .setStartDate(config.getStartDate())
                        .setEndDate(config.getEndDate())
                        .setPositionCount(config.getPositionCount())
                        .setOutputFrame(config.getOutputFrame())
                        .setIntegrator(config.getIntegrator())
                        .build();

                // Call propagation service
                // propagationService.propagateTLE takes the DTO.
                // propagationMapper.toDTO converts Grpc Request to DTO.

                List<TleResult> results = propagationService.propagateTLE(propagationMapper.toDTO(grpcRequest))
                        .collect().asList().await().indefinitely();

                if (results != null && !results.isEmpty()) {
                    TleResult result = results.get(0);
                    batchResponses.add(TLEStreamResponse.newBuilder()
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
                }
            } catch (Exception e) {
                log.error("Error processing TLE in batch: {}", tleLines.getTleLine1(), e);
                batchResponses.add(TLEStreamResponse.newBuilder()
                        .setSatelliteId(extractSatelliteId(tleLines.getTleLine1()))
                        .setError(e.getMessage())
                        .build());
            }
        }
        return batchResponses;
    }

    private int extractSatelliteId(String line1) {
        try {
            return Integer.parseInt(line1.substring(2, 7).trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
