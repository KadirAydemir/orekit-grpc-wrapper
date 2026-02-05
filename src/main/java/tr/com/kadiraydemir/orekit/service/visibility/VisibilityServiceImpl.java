package tr.com.kadiraydemir.orekit.service.visibility;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventsLogger;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import tr.com.kadiraydemir.orekit.service.frame.FrameService;
import tr.com.kadiraydemir.orekit.model.AccessIntervalsRequest;
import tr.com.kadiraydemir.orekit.model.AccessIntervalResult;
import tr.com.kadiraydemir.orekit.model.VisibilityResult;

@ApplicationScoped
public class VisibilityServiceImpl implements VisibilityService {

    @Inject
    FrameService frameService;

    @Override
    public VisibilityResult getAccessIntervals(AccessIntervalsRequest request) {
        // 1. Setup TLE
        TLE tle = new TLE(request.tleLine1(), request.tleLine2());
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);

        // 2. Setup Reference Dates
        AbsoluteDate startDate = new AbsoluteDate(request.startDateIso(), TimeScalesFactory.getUTC());
        AbsoluteDate endDate = new AbsoluteDate(request.endDateIso(), TimeScalesFactory.getUTC());

        // 3. Setup Ground Station Frame
        TopocentricFrame stationFrame = frameService.createTopocentricFrame(
                request.groundStation().latitudeDegrees(),
                request.groundStation().longitudeDegrees(),
                request.groundStation().altitudeMeters(),
                request.groundStation().name());

        // 4. Setup Detector
        double minElevation = java.lang.Math.toRadians(request.minElevationDegrees());
        ElevationDetector detector = new ElevationDetector(stationFrame)
                .withConstantElevation(minElevation)
                .withMaxCheck(60.0) // Check every 60s max
                .withThreshold(1.0e-3) // Convergence threshold
                .withHandler(new ContinueOnEvent()); // Don't stop propagation

        // 5. Monitor Events
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detector));

        // 6. Propagate
        propagator.propagate(startDate, endDate);

        // 7. Process Events
        List<LoggedEvent> events = logger.getLoggedEvents();
        List<AccessIntervalResult> intervals = new ArrayList<>();

        AbsoluteDate currentStart = null;

        // Handle case where we start valid (not easily detected by just logging, but we
        // can check initial state)
        // Simple check: is currently visible at startDate?
        double initialElevation = stationFrame.getElevation(
                propagator.propagate(startDate).getPVCoordinates().getPosition(),
                propagator.getFrame(),
                startDate);
        if (initialElevation > minElevation) {
            currentStart = startDate;
        }

        for (LoggedEvent event : events) {
            if (event.isIncreasing()) {
                // Rising (entering visibility)
                currentStart = event.getState().getDate();
            } else {
                // Setting (leaving visibility)
                if (currentStart != null) {
                    intervals.add(buildInterval(currentStart, event.getState().getDate()));
                    currentStart = null;
                }
            }
        }

        // If still visible at end
        if (currentStart != null) {
            intervals.add(buildInterval(currentStart, endDate));
        }

        return new VisibilityResult(
                tle.getElementNumber() + "",
                request.groundStation().name(),
                intervals);
    }

    private AccessIntervalResult buildInterval(AbsoluteDate start, AbsoluteDate end) {
        return new AccessIntervalResult(
                start.toString(),
                end.toString(),
                end.durationFrom(start));
    }
}
