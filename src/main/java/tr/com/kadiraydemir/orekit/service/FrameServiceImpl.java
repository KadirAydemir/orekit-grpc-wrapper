package tr.com.kadiraydemir.orekit.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.utils.IERSConventions;
import tr.com.kadiraydemir.orekit.grpc.ReferenceFrame;

/**
 * Implementation of FrameService for resolving reference frames
 */
@ApplicationScoped
public class FrameServiceImpl implements FrameService {

    @Override
    public Frame resolveFrame(ReferenceFrame referenceFrame) {
        if (referenceFrame == null) {
            return FramesFactory.getTEME(); // Default
        }
        return switch (referenceFrame) {
            case TEME -> FramesFactory.getTEME();
            case GCRF -> FramesFactory.getGCRF();
            case EME2000 -> FramesFactory.getEME2000();
            case ITRF -> FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            default -> FramesFactory.getTEME();
        };
    }

    @Override
    public Frame getTemeFrame() {
        return FramesFactory.getTEME();
    }
}
