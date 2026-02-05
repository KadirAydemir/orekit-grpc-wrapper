package tr.com.kadiraydemir.orekit.service.propagation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.AdamsBashforthIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince54Integrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.ode.nonstiff.GraggBulirschStoerIntegrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import tr.com.kadiraydemir.orekit.grpc.IntegratorType;
import tr.com.kadiraydemir.orekit.mapper.IntegratorTestMapper;

@QuarkusTest
public class IntegratorServiceImplTest {

    @Inject
    IntegratorService integratorService;

    @Inject
    IntegratorTestMapper integratorTestMapper;

    @Test
    public void testCreateIntegrator() {
        // Use a non-singular orbit (non-zero eccentricity and inclination)
        Orbit orbit = new KeplerianOrbit(7000000.0, 0.001, Math.toRadians(45.0), 0.0, 0.0, 0.0, PositionAngleType.MEAN,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU);

        AbstractIntegrator i1 = integratorService.createIntegrator(integratorTestMapper.map(IntegratorType.DORMAND_PRINCE_853), orbit);
        AbstractIntegrator i2 = integratorService.createIntegrator(integratorTestMapper.map(IntegratorType.DORMAND_PRINCE_54), orbit);
        AbstractIntegrator i3 = integratorService.createIntegrator(integratorTestMapper.map(IntegratorType.CLASSICAL_RUNGE_KUTTA), orbit);
        AbstractIntegrator i4 = integratorService.createIntegrator(integratorTestMapper.map(IntegratorType.ADAMS_BASHFORTH), orbit);
        AbstractIntegrator i5 = integratorService.createIntegrator(integratorTestMapper.map(IntegratorType.ADAMS_MOULTON), orbit);
        AbstractIntegrator i6 = integratorService.createIntegrator(integratorTestMapper.map(IntegratorType.GRAGG_BULIRSCH_STOER), orbit);

        Assertions.assertTrue(i1 instanceof DormandPrince853Integrator);
        Assertions.assertTrue(i2 instanceof DormandPrince54Integrator);
        Assertions.assertTrue(i3 instanceof ClassicalRungeKuttaIntegrator);
        Assertions.assertTrue(i4 instanceof AdamsBashforthIntegrator);
        Assertions.assertTrue(i5 instanceof org.hipparchus.ode.nonstiff.AdamsMoultonIntegrator);
        Assertions.assertTrue(i6 instanceof GraggBulirschStoerIntegrator);
    }
}
