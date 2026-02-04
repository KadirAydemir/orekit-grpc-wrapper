package tr.com.kadiraydemir.orekit.service.visibility;

import tr.com.kadiraydemir.orekit.grpc.AccessIntervalsRequest;
import tr.com.kadiraydemir.orekit.grpc.AccessIntervalsResponse;

/**
 * Service for performing visibility/access analysis
 */
public interface VisibilityService {

    /**
     * Compute access intervals between a satellite and a ground station
     * 
     * @param request the access intervals request
     * @return the access intervals response
     */
    AccessIntervalsResponse getAccessIntervals(AccessIntervalsRequest request);
}
