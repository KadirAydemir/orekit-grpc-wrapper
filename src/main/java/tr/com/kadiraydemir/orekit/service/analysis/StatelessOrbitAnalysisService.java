package tr.com.kadiraydemir.orekit.service.analysis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.Relativity;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.kadiraydemir.orekit.exception.OrekitException;
import tr.com.kadiraydemir.orekit.model.IntegratorType;
import tr.com.kadiraydemir.orekit.model.ReferenceFrameType;
import tr.com.kadiraydemir.orekit.service.frame.FrameService;
import tr.com.kadiraydemir.orekit.service.propagation.IntegratorService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@ApplicationScoped
public class StatelessOrbitAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(StatelessOrbitAnalysisService.class);

    private static final double SPACECRAFT_MASS = 100.0;
    private static final double MANEUVER_THRESHOLD_KM_DEFAULT = 2.0;

    @Inject
    FrameService frameService;

    @Inject
    IntegratorService integratorService;

    public ManeuverDetectionResult detect(
            TLE initialTle,
            List<TLE> observedTles,
            ForceModelConfig config,
            double maneuverThresholdKm,
            ReferenceFrameType outputFrame) {

        try {
            log.info("Starting orbit analysis for satellite {}", initialTle.getSatelliteNumber());

            Frame temeFrame = frameService.getTemeFrame();
            Frame outputFrameRef = frameService.resolveFrame(outputFrame);
            TimeScale utc = TimeScalesFactory.getUTC();

            // Setup propagator with high-fidelity force models
            NumericalPropagator propagator = createNumericalPropagator(initialTle, config, temeFrame);

            // Sort observed TLEs by date
            List<TLE> sortedObservedTles = observedTles.stream()
                    .sorted(Comparator.comparing(TLE::getDate))
                    .toList();

            // Step 1: Analyze maneuvers
            double threshold = maneuverThresholdKm > 0 ? maneuverThresholdKm : MANEUVER_THRESHOLD_KM_DEFAULT;
            List<ManeuverReport> maneuvers = analyzeManeuvers(
                    propagator, sortedObservedTles, threshold, outputFrameRef, utc);

            log.info("Orbit analysis completed. Detected {} maneuvers",
                    maneuvers.size());

            return new ManeuverDetectionResult(maneuvers, outputFrameRef.getName());

        } catch (Exception e) {
            throw new OrekitException("Orbit analysis failed: " + e.getMessage(), e);
        }
    }

    private NumericalPropagator createNumericalPropagator(
            TLE tle, ForceModelConfig config, Frame temeFrame) {

        // Use analytical propagator to get initial state at TLE epoch
        TLEPropagator analyticalPropagator = TLEPropagator.selectExtrapolator(tle);
        PVCoordinates initialPV = analyticalPropagator.getPVCoordinates(tle.getDate(), temeFrame);

        // Create orbit from PV coordinates
        Orbit initialOrbit = new KeplerianOrbit(initialPV, temeFrame, tle.getDate(), Constants.WGS84_EARTH_MU);

        // Configure integrator (use Dormand-Prince 8(5,3) by default)
        AbstractIntegrator integrator = integratorService.createIntegrator(IntegratorType.DORMAND_PRINCE_853,
                initialOrbit);

        // Create numerical propagator
        NumericalPropagator numProp = new NumericalPropagator(integrator);
        numProp.setOrbitType(OrbitType.KEPLERIAN);
        numProp.setAttitudeProvider(new FrameAlignedProvider(temeFrame));

        // Add gravity force model (Holmes-Featherstone)
        int degree = config != null && config.gravityDegree() > 0 ? config.gravityDegree() : 10;
        int order = config != null && config.gravityOrder() > 0 ? config.gravityOrder() : 10;

        NormalizedSphericalHarmonicsProvider gravityProvider = GravityFieldFactory.getNormalizedProvider(degree, order);
        Frame itrfFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        HolmesFeatherstoneAttractionModel gravityModel = new HolmesFeatherstoneAttractionModel(itrfFrame,
                gravityProvider);
        numProp.addForceModel(gravityModel);

        // Add third body attractions (Sun and Moon)
        numProp.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
        numProp.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));

        // Add relativity force model
        numProp.addForceModel(new Relativity(Constants.WGS84_EARTH_MU));

        numProp.setInitialState(new SpacecraftState(initialOrbit, SPACECRAFT_MASS));

        return numProp;
    }

    public List<ManeuverReport> analyzeManeuvers(
            NumericalPropagator propagator,
            List<TLE> observedTles,
            double thresholdKm,
            Frame outputFrame,
            TimeScale utc) {

        if (observedTles.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("Analyzing {} observed TLEs for maneuver detection with threshold {} km",
                observedTles.size(), thresholdKm);

        // Use parallel stream for performance
        return IntStream.range(0, observedTles.size())
                .parallel()
                .mapToObj(i -> analyzeSingleManeuver(propagator, observedTles.get(i), thresholdKm, outputFrame, utc, i))
                .filter(report -> report != null)
                .sorted(Comparator.comparing(ManeuverReport::timestamp))
                .toList();
    }

    private ManeuverReport analyzeSingleManeuver(
            NumericalPropagator propagator,
            TLE observedTle,
            double thresholdKm,
            Frame outputFrame,
            TimeScale utc,
            int index) {

        try {
            AbsoluteDate observedDate = observedTle.getDate();
            String timestamp = observedDate.toString(utc);

            // Propagate to the observed TLE epoch
            PVCoordinates propagatedPV = propagator.getPVCoordinates(observedDate, outputFrame);

            // Get PV from observed TLE
            TLEPropagator observedPropagator = TLEPropagator.selectExtrapolator(observedTle);
            PVCoordinates observedPV = observedPropagator.getPVCoordinates(observedDate, outputFrame);

            // Calculate position residual
            double dx = propagatedPV.getPosition().getX() - observedPV.getPosition().getX();
            double dy = propagatedPV.getPosition().getY() - observedPV.getPosition().getY();
            double dz = propagatedPV.getPosition().getZ() - observedPV.getPosition().getZ();
            double residualKm = FastMath.sqrt(dx * dx + dy * dy + dz * dz) / 1000.0;

            // Calculate velocity residual for delta-V estimate
            double dvx = propagatedPV.getVelocity().getX() - observedPV.getVelocity().getX();
            double dvy = propagatedPV.getVelocity().getY() - observedPV.getVelocity().getY();
            double dvz = propagatedPV.getVelocity().getZ() - observedPV.getVelocity().getZ();
            double deltaVms = FastMath.sqrt(dvx * dvx + dvy * dvy + dvz * dvz);

            if (residualKm > thresholdKm) {
                String description = String.format(
                        "Maneuver detected at %s: position residual = %.2f km, estimated delta-V = %.2f m/s",
                        timestamp, residualKm, deltaVms);

                log.debug("Detected maneuver at index {}: {} km residual", index, residualKm);

                return new ManeuverReport(timestamp, residualKm, deltaVms, description);
            }

            return null;

        } catch (Exception e) {
            log.error("Error analyzing TLE at index {}: {}", index, e.getMessage());
            return null;
        }
    }

    public record ForceModelConfig(int gravityDegree, int gravityOrder, boolean solarRadiationPressure,
            boolean atmosphericDrag) {
    }

    public record ManeuverReport(String timestamp, double residualKm, double deltaVestimateMS, String description) {
    }

    public record ManeuverDetectionResult(List<ManeuverReport> maneuvers, String frame) {
    }
}