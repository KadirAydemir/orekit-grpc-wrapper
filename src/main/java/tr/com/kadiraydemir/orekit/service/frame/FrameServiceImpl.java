package tr.com.kadiraydemir.orekit.service.frame;

import jakarta.enterprise.context.ApplicationScoped;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.hipparchus.util.FastMath;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import tr.com.kadiraydemir.orekit.model.ReferenceFrameType;

/**
 * Implementation of FrameService for resolving reference frames
 */
@ApplicationScoped
public class FrameServiceImpl implements FrameService {

    @Override
    public Frame resolveFrame(ReferenceFrameType referenceFrame) {
        if (referenceFrame == null) {
            return FramesFactory.getTEME(); // Default
        }
        return switch (referenceFrame) {
            case TEME -> FramesFactory.getTEME();
            case GCRF -> FramesFactory.getGCRF();
            case EME2000 -> FramesFactory.getEME2000();
            case ITRF -> FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        };
    }

    @Override
    public Frame getTemeFrame() {
        return FramesFactory.getTEME();
    }

    @Override
    public TopocentricFrame createTopocentricFrame(double latitude, double longitude, double altitude,
            String name) {
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                itrf);

        GeodeticPoint point = new GeodeticPoint(
                FastMath.toRadians(latitude),
                FastMath.toRadians(longitude),
                altitude);

        return new TopocentricFrame(earth, point, name);
    }
}
