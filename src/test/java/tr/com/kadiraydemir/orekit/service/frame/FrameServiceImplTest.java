package tr.com.kadiraydemir.orekit.service.frame;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import tr.com.kadiraydemir.orekit.grpc.ReferenceFrame;

@QuarkusTest
public class FrameServiceImplTest {

    @Inject
    FrameServiceImpl frameService;

    @Test
    public void testResolveFrame() {
        Assertions.assertEquals(FramesFactory.getTEME().getName(), frameService.resolveFrame(null).getName());
        Assertions.assertEquals(FramesFactory.getTEME().getName(), frameService.resolveFrame(ReferenceFrame.TEME).getName());
        Assertions.assertEquals(FramesFactory.getGCRF().getName(), frameService.resolveFrame(ReferenceFrame.GCRF).getName());
        Assertions.assertEquals(FramesFactory.getEME2000().getName(), frameService.resolveFrame(ReferenceFrame.EME2000).getName());
        Assertions.assertTrue(frameService.resolveFrame(ReferenceFrame.ITRF).getName().contains("ITRF"));

        // Default case (if any other enum value) - Protocol buffers enums might be tricky if we don't handle UNRECOGNIZED
        // But the switch covers explicit cases. The default branch in switch covers "default -> FramesFactory.getTEME()"
        // To hit default, we'd need an enum value that isn't one of the cases.
        // ReferenceFrame includes UNRECOGNIZED if it's a proto enum.
        Assertions.assertEquals(FramesFactory.getTEME().getName(), frameService.resolveFrame(ReferenceFrame.UNRECOGNIZED).getName());
    }

    @Test
    public void testGetTemeFrame() {
        Assertions.assertEquals(FramesFactory.getTEME().getName(), frameService.getTemeFrame().getName());
    }

    @Test
    public void testCreateTopocentricFrame() {
        TopocentricFrame station = frameService.createTopocentricFrame(39.9334, 32.8597, 938.0, "Ankara");
        Assertions.assertNotNull(station);
        Assertions.assertEquals("Ankara", station.getName());
        Assertions.assertEquals(Math.toRadians(39.9334), station.getPoint().getLatitude(), 1e-6);
        Assertions.assertEquals(Math.toRadians(32.8597), station.getPoint().getLongitude(), 1e-6);
        Assertions.assertEquals(938.0, station.getPoint().getAltitude(), 1e-6);
    }
}
