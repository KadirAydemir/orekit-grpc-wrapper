package tr.com.kadiraydemir.orekit.service;

import org.orekit.frames.Frame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.TLE;
import tr.com.kadiraydemir.orekit.grpc.IntegratorType;
import tr.com.kadiraydemir.orekit.grpc.PropagationModel;

/**
 * Factory service for creating propagators
 */
public interface PropagatorFactoryService {

    /**
     * Create a propagator based on model selection
     * 
     * @param tle            TLE data
     * @param model          propagation model to use
     * @param integratorType integrator type for numerical propagation
     * @param temeFrame      TEME reference frame
     * @return configured Propagator
     */
    Propagator createPropagator(TLE tle, PropagationModel model, IntegratorType integratorType, Frame temeFrame);

    /**
     * Create a numerical propagator initialized from TLE state
     * 
     * @param tle            TLE data
     * @param integratorType integrator type
     * @param temeFrame      TEME reference frame
     * @return configured NumericalPropagator
     */
    Propagator createNumericalPropagator(TLE tle, IntegratorType integratorType, Frame temeFrame);
}
