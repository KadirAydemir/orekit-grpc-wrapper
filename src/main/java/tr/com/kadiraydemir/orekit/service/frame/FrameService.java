package tr.com.kadiraydemir.orekit.service.frame;

import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
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
