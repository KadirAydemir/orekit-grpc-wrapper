package tr.com.kadiraydemir.orekit.service.tlefitting;

import jakarta.enterprise.context.ApplicationScoped;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Position;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.generation.LeastSquaresTleGenerationAlgorithm;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.propagation.conversion.TLEPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import tr.com.kadiraydemir.orekit.service.frame.FrameService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.kadiraydemir.orekit.model.PositionMeasurement;
import tr.com.kadiraydemir.orekit.model.TleFittingRequest;
import tr.com.kadiraydemir.orekit.model.TleFittingResult;

import java.util.List;

/**
 * Implementation of TleFittingService using Orekit's Batch Least Squares
 * Estimator.
 */
@ApplicationScoped
public class TleFittingServiceImpl implements TleFittingService {

        private static final Logger LOG = LoggerFactory.getLogger(TleFittingServiceImpl.class);

        // Default TLE for initialization when no initial TLE provided (ISS-like orbit)
        // Must be exactly 69 characters per line
        private static final String DEFAULT_TLE_LINE1 = "1 25544U 98067A   24001.50000000  .00000000  00000-0  00000-0 0  0010";
        private static final String DEFAULT_TLE_LINE2 = "2 25544  51.6400  100.0000 0007000 100.0000 260.0000 15.50000000  010";

        @Inject
        FrameService frameService;

        @Override
        public TleFittingResult fitTLE(TleFittingRequest request) {
                try {
                        LOG.info("Starting TLE fitting with {} measurements", request.measurements().size());

                        // Validate input
                        if (request.measurements().size() < 2) {
                                return TleFittingResult.failure("At least 2 measurements required for TLE fitting");
                        }

                        // Get or create initial TLE
                        TLE initialTle = createInitialTle(request);

                        // Create frame (TEME is the native TLE frame)
                        Frame temeFrame = FramesFactory.getTEME();

                        // Create TLE generation algorithm for the builder
                        LeastSquaresTleGenerationAlgorithm generationAlgorithm = new LeastSquaresTleGenerationAlgorithm();

                        // Create TLE propagator builder
                        TLEPropagatorBuilder propagatorBuilder = new TLEPropagatorBuilder(
                                        initialTle,
                                        org.orekit.orbits.PositionAngleType.MEAN,
                                        10.0, // Increased position scale for better convergence stability
                                        generationAlgorithm);

                        // Create the batch least squares estimator
                        BatchLSEstimator estimator = createEstimator(propagatorBuilder, request);

                        // Get input frame
                        Frame inputFrame = frameService.resolveFrame(request.inputFrame());

                        // Add measurements
                        addMeasurements(estimator, request.measurements(), inputFrame, temeFrame);

                        // Run the estimation
                        Propagator[] estimatedPropagators = estimator.estimate();

                        // Extract results
                        if (estimatedPropagators.length == 0) {
                                return TleFittingResult.failure("Estimation failed to produce a propagator");
                        }

                        Propagator estimatedPropagator = estimatedPropagators[0];

                        // Extract the fitted TLE from the propagator using generation algorithm
                        TLE fittedTle = extractFittedTle(estimatedPropagator, initialTle);

                        // Get statistics
                        double rms = estimator.getOptimum() != null
                                        ? Math.sqrt(estimator.getOptimum().getRMS())
                                        : 0.0;
                        int iterations = estimator.getIterationsCount();
                        int evaluations = estimator.getEvaluationsCount();
                        boolean converged = iterations < request.maxIterations();

                        LOG.info("TLE fitting completed: RMS={}, iterations={}, evaluations={}",
                                        rms, iterations, evaluations);

                        return TleFittingResult.success(
                                        fittedTle.getLine1(),
                                        fittedTle.getLine2(),
                                        request.satelliteName(),
                                        rms,
                                        iterations,
                                        evaluations);

                } catch (org.hipparchus.exception.MathRuntimeException e) {
                        LOG.error("TLE fitting failed - Math error", e);
                        return TleFittingResult.failure("TLE fitting failed: " + e.getMessage());
                } catch (org.orekit.errors.OrekitException e) {
                        LOG.error("TLE fitting failed - Orekit error", e);
                        return TleFittingResult.failure("TLE fitting failed: " + e.getMessage());
                } catch (Exception e) {
                        LOG.error("TLE fitting failed - Unexpected error", e);
                        return TleFittingResult.failure("TLE fitting failed: " + e.getMessage());
                }
        }

        /**
         * Creates the initial TLE from the request or generates a default one.
         */
        private TLE createInitialTle(TleFittingRequest request) {
                if (request.initialTleLine1() != null && !request.initialTleLine1().isEmpty()
                                && request.initialTleLine2() != null && !request.initialTleLine2().isEmpty()) {
                        return new TLE(request.initialTleLine1(), request.initialTleLine2());
                }

                // Use default TLE as template
                return new TLE(DEFAULT_TLE_LINE1, DEFAULT_TLE_LINE2);
        }

        /**
         * Creates and configures the batch least squares estimator.
         */
        private BatchLSEstimator createEstimator(PropagatorBuilder propagatorBuilder, TleFittingRequest request) {
                // Configure Levenberg-Marquardt optimizer
                LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();

                // Create estimator with optimizer
                BatchLSEstimator estimator = new BatchLSEstimator(optimizer, propagatorBuilder);

                // Configure convergence criteria
                int maxIterations = Math.max(request.maxIterations(), 100); // Minimum 100 iterations
                estimator.setParametersConvergenceThreshold(request.convergenceThreshold());
                estimator.setMaxIterations(maxIterations);
                estimator.setMaxEvaluations(maxIterations * 2);

                return estimator;
        }

        /**
         * Adds position measurements to the estimator.
         */
        private void addMeasurements(BatchLSEstimator estimator, List<PositionMeasurement> measurements,
                        Frame inputFrame, Frame temeFrame) {
                ObservableSatellite satellite = new ObservableSatellite(0);

                for (PositionMeasurement measurement : measurements) {
                        AbsoluteDate date = new AbsoluteDate(measurement.timestamp(), TimeScalesFactory.getUTC());

                        // Create position vector (in input frame)
                        org.hipparchus.geometry.euclidean.threed.Vector3D position = new org.hipparchus.geometry.euclidean.threed.Vector3D(
                                        measurement.positionX(),
                                        measurement.positionY(),
                                        measurement.positionZ());

                        // Transform position to TEME (native TLE frame) if necessary
                        org.hipparchus.geometry.euclidean.threed.Vector3D temePosition;
                        if (inputFrame.equals(temeFrame)) {
                                temePosition = position;
                        } else {
                                try {
                                        temePosition = inputFrame.getTransformTo(temeFrame, date)
                                                        .transformPosition(position);
                                } catch (org.orekit.errors.OrekitException e) {
                                        LOG.warn("Failed to transform measurement at {} from {} to TEME, using original",
                                                        date, inputFrame.getName());
                                        temePosition = position;
                                }
                        }

                        // Create Position measurement for Orekit (in TEME frame)
                        Position orekitPosition = new Position(
                                        date,
                                        temePosition,
                                        measurement.sigma(),
                                        measurement.weight(),
                                        satellite);

                        estimator.addMeasurement(orekitPosition);
                }

                LOG.debug("Added {} measurements to estimator", measurements.size());

                // Log the first measurement for diagnostic purposes
                if (!measurements.isEmpty()) {
                        PositionMeasurement m = measurements.get(0);
                        LOG.info("Diagnostic - Input Frame: {}", inputFrame.getName());
                        LOG.info("Diagnostic - First measurement: timestamp={}, X={}, Y={}, Z={}",
                                        m.timestamp(), m.positionX(), m.positionY(), m.positionZ());

                        // Also check the distance from Earth center
                        double r = Math.sqrt(m.positionX() * m.positionX() + m.positionY() * m.positionY()
                                        + m.positionZ() * m.positionZ());
                        LOG.info("Diagnostic - First measurement radius: {} meters (Altitude: {} km)", r,
                                        (r - 6378137.0) / 1000.0);
                }
        }

        /**
         * Extracts the fitted TLE from the estimated propagator.
         */
        private TLE extractFittedTle(Propagator propagator, TLE templateTle) {
                // Get the initial state from the propagator
                SpacecraftState state = propagator.getInitialState();

                // Use LeastSquaresTleGenerationAlgorithm to generate TLE from state
                LeastSquaresTleGenerationAlgorithm algorithm = new LeastSquaresTleGenerationAlgorithm();

                return algorithm.generate(state, templateTle);
        }
}
