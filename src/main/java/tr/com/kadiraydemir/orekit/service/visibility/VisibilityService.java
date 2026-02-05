package tr.com.kadiraydemir.orekit.service.visibility;

import tr.com.kadiraydemir.orekit.model.AccessIntervalsRequest;
import tr.com.kadiraydemir.orekit.model.VisibilityResult;

/**
 * Service for performing visibility/access analysis
 */
public interface VisibilityService {

    /**
     * Compute access intervals between a satellite and a ground station
     * 
     * @param request the access intervals request
     * @return the access intervals result
     */
    VisibilityResult getAccessIntervals(AccessIntervalsRequest request);
}
