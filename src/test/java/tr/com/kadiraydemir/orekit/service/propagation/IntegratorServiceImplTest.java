package tr.com.kadiraydemir.orekit.service.propagation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import tr.com.kadiraydemir.orekit.grpc.IntegratorType;

@QuarkusTest
public class IntegratorServiceImplTest {

    @Inject
    IntegratorServiceImpl integratorService;

    private Orbit orbit;

    @BeforeEach
    public void setup() {
        // Non-singular orbit (non-zero inclination)
        orbit = new KeplerianOrbit(7000000.0, 0.001, 0.1, 0, 0, 0, PositionAngleType.MEAN,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU);
    }

    @Test
    public void testCreateIntegrator() {
        // Null -> Default (DP853)
        Assertions.assertTrue(integratorService.createIntegrator(null, orbit) instanceof DormandPrince853Integrator);

        // Enum cases
        Assertions.assertTrue(integratorService.createIntegrator(IntegratorType.DORMAND_PRINCE_853, orbit) instanceof DormandPrince853Integrator);
        Assertions.assertTrue(integratorService.createIntegrator(IntegratorType.DORMAND_PRINCE_54, orbit) instanceof DormandPrince54Integrator);
        Assertions.assertTrue(integratorService.createIntegrator(IntegratorType.CLASSICAL_RUNGE_KUTTA, orbit) instanceof ClassicalRungeKuttaIntegrator);
        Assertions.assertTrue(integratorService.createIntegrator(IntegratorType.ADAMS_BASHFORTH, orbit) instanceof AdamsBashforthIntegrator);
        Assertions.assertTrue(integratorService.createIntegrator(IntegratorType.ADAMS_MOULTON, orbit) instanceof AdamsMoultonIntegrator);
        Assertions.assertTrue(integratorService.createIntegrator(IntegratorType.GRAGG_BULIRSCH_STOER, orbit) instanceof GraggBulirschStoerIntegrator);

        // Default -> Default (DP853)
        Assertions.assertTrue(integratorService.createIntegrator(IntegratorType.UNRECOGNIZED, orbit) instanceof DormandPrince853Integrator);
    }
}
