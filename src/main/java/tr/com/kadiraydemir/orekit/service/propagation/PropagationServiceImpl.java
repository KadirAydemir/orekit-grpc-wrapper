package tr.com.kadiraydemir.orekit.service.propagation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import tr.com.kadiraydemir.orekit.grpc.*;
import tr.com.kadiraydemir.orekit.service.frame.FrameService;
import tr.com.kadiraydemir.orekit.model.OrbitResult;
import tr.com.kadiraydemir.orekit.model.TleResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.smallrye.mutiny.Multi;

/**
 * Implementation of PropagationService for orbital propagation
 */
@ApplicationScoped
public class PropagationServiceImpl implements PropagationService {

    private final FrameService frameService;
    private final PropagatorFactoryService propagatorFactoryService;

    @Inject
    public PropagationServiceImpl(FrameService frameService, PropagatorFactoryService propagatorFactoryService) {
        this.frameService = frameService;
        this.propagatorFactoryService = propagatorFactoryService;
    }

    @Override
    public OrbitResult propagate(PropagateRequest request) {
        try {
            Frame inertialFrame = FramesFactory.getEME2000();
            AbsoluteDate initialDate = new AbsoluteDate(request.getEpochIso(), TimeScalesFactory.getUTC());

            Orbit initialOrbit = new KeplerianOrbit(
                    request.getSemimajorAxis(),
                    request.getEccentricity(),
                    request.getInclination(),
                    request.getPerigeeArgument(),
                    request.getRightAscensionOfAscendingNode(),
                    request.getMeanAnomaly(),
                    PositionAngleType.MEAN,
                    inertialFrame,
                    initialDate,
                    Constants.WGS84_EARTH_MU);

            Propagator propagator = new KeplerianPropagator(initialOrbit);
            AbsoluteDate targetDate = initialDate.shiftedBy(request.getDuration());
            PVCoordinates pv = propagator.getPVCoordinates(targetDate, inertialFrame);

            return new OrbitResult(
                    request.getSatelliteName(),
                    pv.getPosition().getX(),
                    pv.getPosition().getY(),
                    pv.getPosition().getZ(),
                    pv.getVelocity().getX(),
                    pv.getVelocity().getY(),
                    pv.getVelocity().getZ(),
                    targetDate.toString(TimeScalesFactory.getUTC()),
                    inertialFrame.getName());

        } catch (Exception e) {
            throw new RuntimeException("Propagation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Multi<TleResult> propagateTLE(TLEPropagateRequest request) {
        try {
            TLE tle = new TLE(request.getTleLine1(), request.getTleLine2());
            PropagationModel requestedModel = request.getModel();
            ReferenceFrame requestedFrame = request.getOutputFrame();

            // Get the output frame (default is TEME)
            Frame outputFrame = frameService.resolveFrame(requestedFrame);

            // Native TLE frame is always TEME
            Frame temeFrame = frameService.getTemeFrame();

            // Get integrator type for numerical model
            IntegratorType integratorType = request.getIntegrator();

            // Create propagator based on model selection
            Propagator propagator = propagatorFactoryService.createPropagator(
                    tle, requestedModel, integratorType, temeFrame);

            AbsoluteDate startDate = new AbsoluteDate(request.getStartDate(), TimeScalesFactory.getUTC());
            AbsoluteDate endDate = new AbsoluteDate(request.getEndDate(), TimeScalesFactory.getUTC());
            int positionCount = request.getPositionCount();

            double duration = endDate.durationFrom(startDate);
            double timeStep = (positionCount > 1) ? duration / (positionCount - 1) : 0;

            String frameName = outputFrame.getName();
            TimeScale utc = TimeScalesFactory.getUTC();

            int batchSize = 100;
            return Multi.createFrom().range(0, (positionCount + batchSize - 1) / batchSize)
                    .map(batchIndex -> {
                        List<TleResult.PositionPointResult> batch = new ArrayList<>(batchSize);
                        int start = batchIndex * batchSize;
                        int end = Math.min(start + batchSize, positionCount);

                        for (int i = start; i < end; i++) {
                            AbsoluteDate currentDate = startDate.shiftedBy(i * timeStep);
                            PVCoordinates pv = propagator.getPVCoordinates(currentDate, outputFrame);
                            batch.add(new TleResult.PositionPointResult(
                                    pv.getPosition().getX(),
                                    pv.getPosition().getY(),
                                    pv.getPosition().getZ(),
                                    currentDate.toString(utc)));
                        }
                        return new TleResult(batch, frameName);
                    });

        } catch (Exception e) {
            return Multi.createFrom().failure(new RuntimeException("TLE Propagation failed: " + e.getMessage(), e));
        }
    }
}
