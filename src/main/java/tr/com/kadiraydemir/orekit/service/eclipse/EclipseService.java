package tr.com.kadiraydemir.orekit.service.eclipse;

import java.util.List;

import tr.com.kadiraydemir.orekit.model.EclipseRequest;
import tr.com.kadiraydemir.orekit.model.EclipseResult;

/**
 * Service for calculating eclipse intervals
 */
public interface EclipseService {

    /**
     * Calculate eclipse intervals for a satellite.
     *
     * @param request the eclipse calculation request
     * @return the eclipse result containing intervals
     */
    EclipseResult calculateEclipses(EclipseRequest request);

    /**
     * Calculate eclipse intervals for multiple satellites.
     *
     * @param tlePairs list of TLE pairs (line1, line2)
     * @param startDateIso start date in ISO format
     * @param endDateIso end date in ISO format
     * @return list of eclipse results for each satellite
     */
    List<EclipseResult> calculateEclipsesBulk(
            List<TLEPair> tlePairs,
            String startDateIso,
            String endDateIso);

    /**
     * Record representing a TLE pair
     */
    record TLEPair(String line1, String line2) {}
}
