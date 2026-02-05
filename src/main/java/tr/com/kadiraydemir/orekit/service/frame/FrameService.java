package tr.com.kadiraydemir.orekit.service.frame;

import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import tr.com.kadiraydemir.orekit.model.ReferenceFrameType;

/**
 * Service for resolving reference frames
 */
public interface FrameService {

    /**
     * Resolve ReferenceFrameType enum to Orekit Frame
     * 
     * @param referenceFrame the requested reference frame type
     * @return the corresponding Orekit Frame
     */
    Frame resolveFrame(ReferenceFrameType referenceFrame);

    /**
     * Get the TEME frame (used as native TLE frame)
     * 
     * @return TEME frame
     */
    Frame getTemeFrame();

    /**
     * Create a TopocentricFrame for a ground station
     * 
     * @param latitude  latitude in degrees
     * @param longitude longitude in degrees
     * @param altitude  altitude in meters
     * @param name      name of the station
     * @return TopocentricFrame
     */
    TopocentricFrame createTopocentricFrame(double latitude, double longitude, double altitude,

            String name);
}
