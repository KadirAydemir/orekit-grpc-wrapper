package tr.com.kadiraydemir.orekit.service.frame;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import tr.com.kadiraydemir.orekit.grpc.ReferenceFrame;
import tr.com.kadiraydemir.orekit.mapper.FrameTestMapper;

@QuarkusTest
public class FrameServiceImplTest {

    @Inject
    FrameService frameService;

    @Inject
    FrameTestMapper frameTestMapper;

    @Test
    public void testResolveFrame() {
        Frame teme = frameService.resolveFrame(frameTestMapper.map(ReferenceFrame.TEME));
        Frame gcrf = frameService.resolveFrame(frameTestMapper.map(ReferenceFrame.GCRF));
        Frame eme2000 = frameService.resolveFrame(frameTestMapper.map(ReferenceFrame.EME2000));
        Frame itrf = frameService.resolveFrame(frameTestMapper.map(ReferenceFrame.ITRF));

        Assertions.assertNotNull(teme);
        Assertions.assertNotNull(gcrf);
        Assertions.assertNotNull(eme2000);
        Assertions.assertNotNull(itrf);
        Assertions.assertEquals("TEME", teme.getName());
    }

    @Test
    public void testCreateTopocentricFrame() {
        TopocentricFrame station = frameService.createTopocentricFrame(39.9334, 32.8597, 1000.0, "Ankara");
        Assertions.assertNotNull(station);
        Assertions.assertEquals("Ankara", station.getName());
    }
}
