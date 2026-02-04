package tr.com.kadiraydemir.orekit.service.propagation;

import jakarta.enterprise.context.ApplicationScoped;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.AdamsBashforthIntegrator;
import org.hipparchus.ode.nonstiff.AdamsMoultonIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince54Integrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.ode.nonstiff.GraggBulirschStoerIntegrator;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.CartesianToleranceProvider;
import org.orekit.propagation.ToleranceProvider;
import tr.com.kadiraydemir.orekit.grpc.IntegratorType;

/**
 * Implementation of IntegratorService for creating numerical integrators
 */
@ApplicationScoped
public class IntegratorServiceImpl implements IntegratorService {

        private static final double MIN_STEP = 0.001;
        private static final double MAX_STEP = 1000;
        private static final double POSITION_TOLERANCE = 1.0; // Position tolerance in meters
        private static final double VELOCITY_TOLERANCE = 0.001; // Velocity tolerance in m/s
        private static final double MASS_TOLERANCE = 1.0e-6; // Mass tolerance in kg
        private static final double FIXED_STEP_SIZE = 60.0; // Fixed step size in seconds
        private static final int ADAMS_ORDER = 4; // Order for Adams integrators

        @Override
        public AbstractIntegrator createIntegrator(IntegratorType integratorType, Orbit initialOrbit) {
                double[][] tolerance = ToleranceProvider.of(
                                CartesianToleranceProvider.of(POSITION_TOLERANCE, VELOCITY_TOLERANCE, MASS_TOLERANCE))
                                .getTolerances(initialOrbit, OrbitType.CARTESIAN, PositionAngleType.TRUE);

                if (integratorType == null) {
                        integratorType = IntegratorType.DORMAND_PRINCE_853;
                }

                return switch (integratorType) {
                        case DORMAND_PRINCE_853 -> new DormandPrince853Integrator(
                                        MIN_STEP, MAX_STEP, tolerance[0], tolerance[1]);
                        case DORMAND_PRINCE_54 -> new DormandPrince54Integrator(
                                        MIN_STEP, MAX_STEP, tolerance[0], tolerance[1]);
                        case CLASSICAL_RUNGE_KUTTA -> new ClassicalRungeKuttaIntegrator(FIXED_STEP_SIZE);
                        case ADAMS_BASHFORTH -> new AdamsBashforthIntegrator(
                                        ADAMS_ORDER, MIN_STEP, MAX_STEP, tolerance[0], tolerance[1]);
                        case ADAMS_MOULTON -> new AdamsMoultonIntegrator(
                                        ADAMS_ORDER, MIN_STEP, MAX_STEP, tolerance[0], tolerance[1]);
                        case GRAGG_BULIRSCH_STOER -> new GraggBulirschStoerIntegrator(
                                        MIN_STEP, MAX_STEP, tolerance[0], tolerance[1]);
                        default -> new DormandPrince853Integrator(
                                        MIN_STEP, MAX_STEP, tolerance[0], tolerance[1]);
                };
        }
}
