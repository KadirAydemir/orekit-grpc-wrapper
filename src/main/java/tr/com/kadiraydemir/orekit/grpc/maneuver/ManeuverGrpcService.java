package tr.com.kadiraydemir.orekit.grpc.maneuver;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.kadiraydemir.orekit.model.ReferenceFrameType;
import tr.com.kadiraydemir.orekit.service.analysis.StatelessOrbitAnalysisService;
import org.orekit.propagation.analytical.tle.TLE;

import java.util.List;

@GrpcService
@RunOnVirtualThread
public class ManeuverGrpcService extends ManeuverServiceGrpc.ManeuverServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(ManeuverGrpcService.class);

    @Inject
    StatelessOrbitAnalysisService analysisService;

    @Override
    public void detectManeuvers(ManeuverDetectionRequest request,
            StreamObserver<ManeuverDetectionResponse> responseObserver) {
        try {
            // Convert initial TLE
            TLE initialTle = new TLE(
                    request.getInitialTle().getTleLine1(),
                    request.getInitialTle().getTleLine2());

            // Convert observed TLEs
            List<TLE> observedTles = request.getObservedTlesList().stream()
                    .map(tleLines -> {
                        try {
                            return new TLE(tleLines.getTleLine1(), tleLines.getTleLine2());
                        } catch (Exception e) {
                            log.error("Error parsing observed TLE: {} {}",
                                    tleLines.getTleLine1(), tleLines.getTleLine2(), e);
                            return null;
                        }
                    })
                    .filter(tle -> tle != null)
                    .toList();

            // Convert force model config
            StatelessOrbitAnalysisService.ForceModelConfig config = null;
            if (request.hasConfig()) {
                ForceModelConfig grpcConfig = request.getConfig();
                config = new StatelessOrbitAnalysisService.ForceModelConfig(
                        grpcConfig.getGravityDegree(),
                        grpcConfig.getGravityOrder(),
                        grpcConfig.getSolarRadiationPressure(),
                        grpcConfig.getAtmosphericDrag());
            }

            // Convert output frame
            ReferenceFrameType outputFrame = ReferenceFrameType.TEME; // Default
            switch (request.getOutputFrame()) {
                case GCRF:
                    outputFrame = ReferenceFrameType.GCRF;
                    break;
                case EME2000:
                    outputFrame = ReferenceFrameType.EME2000;
                    break;
                case ITRF:
                    outputFrame = ReferenceFrameType.ITRF;
                    break;
                default:
                    outputFrame = ReferenceFrameType.TEME;
            }

            // Call analysis service
            StatelessOrbitAnalysisService.ManeuverDetectionResult result = analysisService.detect(
                    initialTle,
                    observedTles,
                    config,
                    request.getManeuverThresholdKm(),
                    outputFrame);

            // Build response
            ManeuverDetectionResponse.Builder responseBuilder = ManeuverDetectionResponse.newBuilder()
                    .setFrame(result.frame());

            // Add maneuvers
            result.maneuvers().forEach(maneuver -> {
                responseBuilder.addManeuvers(ManeuverReport.newBuilder()
                        .setTimestamp(maneuver.timestamp())
                        .setResidualKm(maneuver.residualKm())
                        .setDeltaVEstimateMS(maneuver.deltaVestimateMS())
                        .setDescription(maneuver.description())
                        .build());
            });

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error detecting maneuvers", e);
            responseObserver.onError(e);
        }
    }
}