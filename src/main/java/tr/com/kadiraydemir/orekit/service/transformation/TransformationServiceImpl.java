package tr.com.kadiraydemir.orekit.service.transformation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import tr.com.kadiraydemir.orekit.model.TransformRequest;
import tr.com.kadiraydemir.orekit.service.frame.FrameService;
import tr.com.kadiraydemir.orekit.model.TransformResult;

@ApplicationScoped
public class TransformationServiceImpl implements TransformationService {

    @Inject
    FrameService frameService;

    @Override
    public TransformResult transform(TransformRequest request) {
        // Resolve frames
        Frame sourceFrame = frameService.resolveFrame(request.sourceFrame());
        Frame targetFrame = frameService.resolveFrame(request.targetFrame());

        // Resolve date
        // Assuming UTC for simplification, ideally could be configurable or detected
        AbsoluteDate epoch = new AbsoluteDate(request.epochIso(), TimeScalesFactory.getUTC());

        // Get transform
        Transform transform = sourceFrame.getTransformTo(targetFrame, epoch);

        // input PV
        Vector3D p = new Vector3D(request.x(), request.y(), request.z());
        Vector3D v = new Vector3D(request.vx(), request.vy(), request.vz());
        PVCoordinates sourcePV = new PVCoordinates(p, v);

        // Transform PV
        PVCoordinates targetPV = transform.transformPVCoordinates(sourcePV);

        return new TransformResult(
                request.sourceFrame().name(),
                request.targetFrame().name(),
                request.epochIso(),
                targetPV.getPosition().getX(),
                targetPV.getPosition().getY(),
                targetPV.getPosition().getZ(),
                targetPV.getVelocity().getX(),
                targetPV.getVelocity().getY(),
                targetPV.getVelocity().getZ());
    }
}
