package tr.com.kadiraydemir.orekit.service.propagation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.DeepSDP4;
import org.orekit.propagation.analytical.tle.SGP4;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.numerical.NumericalPropagator;
import tr.com.kadiraydemir.orekit.grpc.IntegratorType;
import tr.com.kadiraydemir.orekit.grpc.PropagationModel;

@QuarkusTest
public class PropagatorFactoryServiceImplTest {

    @Inject
    PropagatorFactoryServiceImpl factoryService;

    private TLE tle;
    private Frame teme;

    @BeforeEach
    public void setup() {
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";
        tle = new TLE(line1, line2);
        teme = FramesFactory.getTEME();
    }

    @Test
    public void testCreatePropagator() {
        // Null -> AUTO (Might be SGP4 or DeepSDP4 depending on Orekit internals)
        Assertions.assertTrue(factoryService.createPropagator(tle, null, null, teme) instanceof org.orekit.propagation.analytical.tle.TLEPropagator);

        // Explicit SGP4
        Assertions.assertEquals(SGP4.class, factoryService.createPropagator(tle, PropagationModel.SGP4, null, teme).getClass());

        // Explicit SDP4
        Assertions.assertEquals(DeepSDP4.class, factoryService.createPropagator(tle, PropagationModel.SDP4, null, teme).getClass());

        // Numerical
        Assertions.assertEquals(NumericalPropagator.class, factoryService.createPropagator(tle, PropagationModel.NUMERICAL, IntegratorType.DORMAND_PRINCE_853, teme).getClass());

        // AUTO -> SGP4 for this TLE
        Assertions.assertTrue(factoryService.createPropagator(tle, PropagationModel.AUTO, null, teme) instanceof org.orekit.propagation.analytical.tle.TLEPropagator);

        // Default -> AUTO
        Assertions.assertTrue(factoryService.createPropagator(tle, PropagationModel.UNRECOGNIZED, null, teme) instanceof org.orekit.propagation.analytical.tle.TLEPropagator);
    }
}
