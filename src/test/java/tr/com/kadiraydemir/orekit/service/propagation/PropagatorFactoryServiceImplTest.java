package tr.com.kadiraydemir.orekit.service.propagation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.TLE;
import tr.com.kadiraydemir.orekit.grpc.IntegratorType;
import tr.com.kadiraydemir.orekit.grpc.PropagationModel;
import tr.com.kadiraydemir.orekit.mapper.PropagationTestMapper;

@QuarkusTest
public class PropagatorFactoryServiceImplTest {

    @Inject
    PropagatorFactoryService factoryService;

    @Inject
    PropagationTestMapper propagationTestMapper;

    @Test
    public void testCreatePropagator() {
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";
        TLE tle = new TLE(line1, line2);

        Propagator p1 = factoryService.createPropagator(tle, propagationTestMapper.map(PropagationModel.SGP4), null, FramesFactory.getTEME());
        Assertions.assertNotNull(p1);
        Assertions.assertTrue(p1 instanceof org.orekit.propagation.analytical.tle.SGP4);

        Propagator p2 = factoryService.createPropagator(tle, propagationTestMapper.map(PropagationModel.SDP4), null, FramesFactory.getTEME());
        Assertions.assertNotNull(p2);
        Assertions.assertTrue(p2 instanceof org.orekit.propagation.analytical.tle.DeepSDP4);

        Propagator p3 = factoryService.createPropagator(tle, propagationTestMapper.map(PropagationModel.NUMERICAL), propagationTestMapper.map(IntegratorType.DORMAND_PRINCE_853),
                FramesFactory.getTEME());
        Assertions.assertNotNull(p3);
        Assertions.assertTrue(p3 instanceof org.orekit.propagation.numerical.NumericalPropagator);

        Propagator p4 = factoryService.createPropagator(tle, propagationTestMapper.map(PropagationModel.AUTO), null, FramesFactory.getTEME());
        Assertions.assertNotNull(p4);
        Assertions.assertTrue(p4 instanceof org.orekit.propagation.analytical.tle.TLEPropagator);

        Propagator p5 = factoryService.createPropagator(tle, null, null, FramesFactory.getTEME());
        Assertions.assertNotNull(p5);
        Assertions.assertTrue(p5 instanceof org.orekit.propagation.analytical.tle.TLEPropagator);
    }
}
