package tr.com.kadiraydemir.orekit.service.tlefitting;

import tr.com.kadiraydemir.orekit.model.TleFittingRequest;
import tr.com.kadiraydemir.orekit.model.TleFittingResult;

/**
 * Service for fitting TLEs to observed measurements using least squares estimation.
 */
public interface TleFittingService {

        /**
         * Fits a TLE to a set of position measurements using Batch Least Squares.
         *
         * @param request the fitting request containing measurements and optional initial TLE
         * @return the fitting result containing the fitted TLE and statistics
         */
        TleFittingResult fitTLE(TleFittingRequest request);
}
