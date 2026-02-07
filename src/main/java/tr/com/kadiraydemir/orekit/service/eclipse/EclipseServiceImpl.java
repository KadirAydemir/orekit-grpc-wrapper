package tr.com.kadiraydemir.orekit.service.eclipse;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.propagation.events.EventsLogger;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.OccultationEngine;

import tr.com.kadiraydemir.orekit.model.EclipseRequest;
import tr.com.kadiraydemir.orekit.model.EclipseIntervalResult;
import tr.com.kadiraydemir.orekit.model.EclipseResult;

@ApplicationScoped
public class EclipseServiceImpl implements EclipseService {

    @Override
    public EclipseResult calculateEclipses(EclipseRequest request) {
        // 1. Setup TLE
        TLE tle = new TLE(request.tleLine1(), request.tleLine2());
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);

        // 2. Setup Reference Dates
        AbsoluteDate startDate = new AbsoluteDate(request.startDateIso(), TimeScalesFactory.getUTC());
        AbsoluteDate endDate = new AbsoluteDate(request.endDateIso(), TimeScalesFactory.getUTC());

        // 3. Setup Bodies
        CelestialBody sun = CelestialBodyFactory.getSun();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        // 4. Setup Eclipse Detector
        // Detects when the satellite enters the shadow (Umbra by default if not split)
        // EclipseDetector checks: occuled (Sun) blocked by occulting (Earth)
        OccultationEngine engine = new OccultationEngine(sun, Constants.SUN_RADIUS, earth);

        EclipseDetector detector = new EclipseDetector(engine)
                .withMaxCheck(60.0)
                .withThreshold(1.0e-3)
                .withHandler(new ContinueOnEvent());

        // 6. Propagate
        // Propagate to start first to get initial state
        SpacecraftState initialState = propagator.propagate(startDate);

        // 5. Monitor Events
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detector));

        // Check if initially in eclipse
        // g > 0: sun is visible (not eclipsed)
        // g < 0: sun is occulted (eclipsed)
        double initialG = detector.g(initialState);
        AbsoluteDate currentStart = null;
        if (initialG < 0.0) {
            currentStart = startDate;
        }

        // Propagate to end
        propagator.propagate(endDate);

        // 7. Process Events
        List<LoggedEvent> events = logger.getLoggedEvents();
        List<EclipseIntervalResult> intervals = new ArrayList<>();

        for (LoggedEvent event : events) {
            if (!event.isIncreasing()) {
                // Positive -> Negative: Entering Eclipse
                currentStart = event.getState().getDate();
            } else {
                // Negative -> Positive: Exiting Eclipse
                if (currentStart != null) {
                    intervals.add(buildInterval(currentStart, event.getState().getDate()));
                    currentStart = null;
                }
            }
        }

        // If still in eclipse at end
        if (currentStart != null) {
            intervals.add(buildInterval(currentStart, endDate));
        }

        return new EclipseResult(
                tle.getSatelliteNumber(),
                intervals);
    }

    private EclipseIntervalResult buildInterval(AbsoluteDate start, AbsoluteDate end) {
        return new EclipseIntervalResult(
                start.toString(),
                end.toString(),
                end.durationFrom(start));
    }

    @Override
    public List<EclipseResult> calculateEclipsesBulk(
            List<TLEPair> tlePairs,
            String startDateIso,
            String endDateIso) {
        List<EclipseResult> results = new ArrayList<>();

        for (TLEPair tlePair : tlePairs) {
            try {
                EclipseRequest request = new EclipseRequest(
                        tlePair.line1(),
                        tlePair.line2(),
                        startDateIso,
                        endDateIso
                );
                EclipseResult result = calculateEclipses(request);
                results.add(result);
            } catch (Exception e) {
                // Log error and continue with next satellite
                // Skip invalid TLEs
                System.err.println("Error processing TLE for satellite: " + e.getMessage());
            }
        }

        return results;
    }
}
