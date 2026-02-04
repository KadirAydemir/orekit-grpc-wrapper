package tr.com.kadiraydemir.orekit.service.transformation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import tr.com.kadiraydemir.orekit.grpc.TransformRequest;
import tr.com.kadiraydemir.orekit.service.frame.FrameService;
import tr.com.kadiraydemir.orekit.grpc.TransformResponse;

@ApplicationScoped
public class TransformationServiceImpl implements TransformationService {

    @Inject
    FrameService frameService;

    @Override
    public TransformResponse transform(TransformRequest request) {
        // Resolve frames
        Frame sourceFrame = frameService.resolveFrame(request.getSourceFrame());
        Frame targetFrame = frameService.resolveFrame(request.getTargetFrame());

        // Resolve date
        // Assuming UTC for simplification, ideally could be configurable or detected
        AbsoluteDate epoch = new AbsoluteDate(request.getEpochIso(), TimeScalesFactory.getUTC());

        // Get transform
        Transform transform = sourceFrame.getTransformTo(targetFrame, epoch);

        // input PV
        Vector3D p = new Vector3D(request.getX(), request.getY(), request.getZ());
        Vector3D v = new Vector3D(request.getVx(), request.getVy(), request.getVz());
        PVCoordinates sourcePV = new PVCoordinates(p, v);

        // Transform PV
        PVCoordinates targetPV = transform.transformPVCoordinates(sourcePV);

        return TransformResponse.newBuilder()
                .setSourceFrame(request.getSourceFrame())
                .setTargetFrame(request.getTargetFrame())
                .setEpochIso(request.getEpochIso())
                .setX(targetPV.getPosition().getX())
                .setY(targetPV.getPosition().getY())
                .setZ(targetPV.getPosition().getZ())
                .setVx(targetPV.getVelocity().getX())
                .setVy(targetPV.getVelocity().getY())
                .setVz(targetPV.getVelocity().getZ())
                .build();
    }
}
