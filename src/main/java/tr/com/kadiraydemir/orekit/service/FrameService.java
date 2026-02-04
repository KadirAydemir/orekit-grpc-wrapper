package tr.com.kadiraydemir.orekit.service;

import org.orekit.frames.Frame;
import tr.com.kadiraydemir.orekit.grpc.ReferenceFrame;

/**
 * Service for resolving reference frames
 */
public interface FrameService {

    /**
     * Resolve gRPC ReferenceFrame enum to Orekit Frame
     * 
     * @param referenceFrame the requested reference frame
     * @return the corresponding Orekit Frame
     */
    Frame resolveFrame(ReferenceFrame referenceFrame);

    /**
     * Get the TEME frame (used as native TLE frame)
     * 
     * @return TEME frame
     */
    Frame getTemeFrame();
}
