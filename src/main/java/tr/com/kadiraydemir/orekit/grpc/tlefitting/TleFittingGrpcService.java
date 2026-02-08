package tr.com.kadiraydemir.orekit.grpc.tlefitting;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.kadiraydemir.orekit.grpc.FitTLERequest;
import tr.com.kadiraydemir.orekit.grpc.FitTLEResponse;
import tr.com.kadiraydemir.orekit.grpc.FitStatistics;
import tr.com.kadiraydemir.orekit.grpc.TleFittingServiceGrpc;
import tr.com.kadiraydemir.orekit.mapper.TleFittingMapper;
import tr.com.kadiraydemir.orekit.model.TleFittingRequest;
import tr.com.kadiraydemir.orekit.model.TleFittingResult;
import tr.com.kadiraydemir.orekit.service.tlefitting.TleFittingService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * gRPC service for TLE fitting operations.
 */
@GrpcService
@RunOnVirtualThread
public class TleFittingGrpcService extends TleFittingServiceGrpc.TleFittingServiceImplBase {

        private static final Logger LOG = LoggerFactory.getLogger(TleFittingGrpcService.class);

        @Inject
        TleFittingService tleFittingService;

        @Inject
        TleFittingMapper tleFittingMapper;

        private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        @Override
        public void fitTLE(FitTLERequest request, StreamObserver<FitTLEResponse> responseObserver) {
                LOG.info("Received fitTLE request for satellite: {}", request.getSatelliteName());

                Uni.createFrom().item(() -> {
                                TleFittingRequest domainRequest = tleFittingMapper.toDomain(request);
                                return tleFittingService.fitTLE(domainRequest);
                        })
                        .runSubscriptionOn(executor)
                        .map(result -> {
                                if (result.error() != null) {
                                        LOG.error("TLE fitting failed: {}", result.error());
                                        return FitTLEResponse.newBuilder()
                                                        .setError(result.error())
                                                        .build();
                                }

                                FitStatistics stats = FitStatistics.newBuilder()
                                                .setRms(result.rms())
                                                .setIterations(result.iterations())
                                                .setConverged(result.converged())
                                                .setEvaluations(result.evaluations())
                                                .build();

                                return FitTLEResponse.newBuilder()
                                                .setFittedTleLine1(result.fittedTleLine1())
                                                .setFittedTleLine2(result.fittedTleLine2())
                                                .setSatelliteName(result.satelliteName())
                                                .setStatistics(stats)
                                                .build();
                        })
                        .subscribe().with(
                                        response -> {
                                                responseObserver.onNext(response);
                                                responseObserver.onCompleted();
                                        },
                                        error -> {
                                                LOG.error("Error in fitTLE", error);
                                                responseObserver.onError(error);
                                        });
        }
}
