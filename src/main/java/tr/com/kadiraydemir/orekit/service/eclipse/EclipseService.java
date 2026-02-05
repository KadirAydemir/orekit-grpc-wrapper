package tr.com.kadiraydemir.orekit.service.eclipse;

import tr.com.kadiraydemir.orekit.grpc.EclipseRequest;
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
}
