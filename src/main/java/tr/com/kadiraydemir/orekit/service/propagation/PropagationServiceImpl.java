package tr.com.kadiraydemir.orekit.service.propagation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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
import tr.com.kadiraydemir.orekit.exception.OrekitException;
import tr.com.kadiraydemir.orekit.model.IntegratorType;
import tr.com.kadiraydemir.orekit.model.OrbitResult;
import tr.com.kadiraydemir.orekit.model.PropagateRequest;
import tr.com.kadiraydemir.orekit.model.PropagateTLEListRequest;
import tr.com.kadiraydemir.orekit.model.PropagationModelType;
import tr.com.kadiraydemir.orekit.model.ReferenceFrameType;
import tr.com.kadiraydemir.orekit.model.TLEPropagateRequest;
import tr.com.kadiraydemir.orekit.model.TleData;
import tr.com.kadiraydemir.orekit.model.TleIndexedResult;
import tr.com.kadiraydemir.orekit.model.TleResult;
import tr.com.kadiraydemir.orekit.service.frame.FrameService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import io.smallrye.mutiny.Multi;

/**
 * Implementation of PropagationService for orbital propagation
 */
@ApplicationScoped
public class PropagationServiceImpl implements PropagationService {

    @Inject
    FrameService frameService;

    @Inject
    PropagatorFactoryService propagatorFactoryService;

    @Inject
    @Named("propagationExecutor")
    ExecutorService propagationExecutor;

    @Override
    public OrbitResult propagate(PropagateRequest request) {
        try {
            Frame inertialFrame = FramesFactory.getEME2000();
            AbsoluteDate initialDate = new AbsoluteDate(request.epochIso(), TimeScalesFactory.getUTC());

            Orbit initialOrbit = new KeplerianOrbit(
                    request.semimajorAxis(),
                    request.eccentricity(),
                    request.inclination(),
                    request.perigeeArgument(),
                    request.rightAscensionOfAscendingNode(),
                    request.meanAnomaly(),
                    PositionAngleType.MEAN,
                    inertialFrame,
                    initialDate,
                    Constants.WGS84_EARTH_MU);

            Propagator propagator = new KeplerianPropagator(initialOrbit);
            AbsoluteDate targetDate = initialDate.shiftedBy(request.duration());
            PVCoordinates pv = propagator.getPVCoordinates(targetDate, inertialFrame);

            return new OrbitResult(
                    request.satelliteName(),
                    pv.getPosition().getX(),
                    pv.getPosition().getY(),
                    pv.getPosition().getZ(),
                    pv.getVelocity().getX(),
                    pv.getVelocity().getY(),
                    pv.getVelocity().getZ(),
                    targetDate.toString(TimeScalesFactory.getUTC()),
                    inertialFrame.getName());

        } catch (Exception e) {
            throw new OrekitException("Propagation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Multi<TleResult> propagateTLE(TLEPropagateRequest request) {
        try {
            TLE tle = new TLE(request.tleLine1(), request.tleLine2());
            AbsoluteDate startDate = new AbsoluteDate(request.startDate(), TimeScalesFactory.getUTC());
            AbsoluteDate endDate = new AbsoluteDate(request.endDate(), TimeScalesFactory.getUTC());
            return propagateSingleTLEInternal(tle, request.model(), request.outputFrame(), request.integrator(),
                    startDate, endDate, request.positionCount());
        } catch (Exception e) {
            return Multi.createFrom().failure(new OrekitException("TLE Propagation failed: " + e.getMessage(), e));
        }
    }

    @Override
    public Multi<TleIndexedResult> propagateTLEList(PropagateTLEListRequest request) {
        try {
            List<TleData> tles = request.tles();
            AbsoluteDate startDate = new AbsoluteDate(request.startDate(), TimeScalesFactory.getUTC());
            AbsoluteDate endDate = new AbsoluteDate(request.endDate(), TimeScalesFactory.getUTC());

            return Multi.createFrom().range(0, tles.size())
                    .emitOn(propagationExecutor)
                    .flatMap(i -> {
                        try {
                            TleData tleData = tles.get(i);
                            TLE tle = new TLE(tleData.line1(), tleData.line2());
                            return propagateSingleTLEInternal(tle, request.model(), request.outputFrame(),
                                    request.integrator(), startDate, endDate,
                                    request.positionCount())
                                    .map(result -> new TleIndexedResult(i, result.positions(), result.frame()));
                        } catch (Exception e) {
                            return Multi.createFrom().failure(new OrekitException("Single TLE propagation in list failed: " + e.getMessage(), e));
                        }
                    });

        } catch (Exception e) {
            return Multi.createFrom().failure(new OrekitException("TLE List Propagation failed: " + e.getMessage(), e));
        }
    }

    private Multi<TleResult> propagateSingleTLEInternal(TLE tle, PropagationModelType requestedModel,
            ReferenceFrameType requestedFrame, IntegratorType integratorType, AbsoluteDate startDate, AbsoluteDate endDate,
            int positionCount) {
        // Get the output frame (default is TEME)
        Frame outputFrame = frameService.resolveFrame(requestedFrame);

        // Native TLE frame is always TEME
        Frame temeFrame = frameService.getTemeFrame();

        // Create propagator based on model selection
        Propagator propagator = propagatorFactoryService.createPropagator(
                tle, requestedModel, integratorType, temeFrame);

        double duration = endDate.durationFrom(startDate);
        double timeStep = (positionCount > 1) ? duration / (positionCount - 1) : 0;

        String frameName = outputFrame.getName();
        TimeScale utc = TimeScalesFactory.getUTC();

        return Multi.createFrom().range(0, positionCount)
                .emitOn(propagationExecutor)
                .map(i -> {
                    AbsoluteDate currentDate = startDate.shiftedBy(i * timeStep);
                    PVCoordinates pv = propagator.getPVCoordinates(currentDate, outputFrame);
                    return new TleResult.PositionPointResult(
                            pv.getPosition().getX(),
                            pv.getPosition().getY(),
                            pv.getPosition().getZ(),
                            currentDate.toString(utc));
                })
                .group().intoLists().of(100)
                .map(positions -> new TleResult(positions, frameName));
    }

}
