package tr.com.kadiraydemir.orekit.service;

import org.hipparchus.ode.AbstractIntegrator;
import org.orekit.orbits.Orbit;
import tr.com.kadiraydemir.orekit.grpc.IntegratorType;

/**
 * Service for creating numerical integrators
 */
public interface IntegratorService {

    /**
     * Create an integrator based on user selection
     * 
     * @param integratorType the type of integrator to create
     * @param initialOrbit   the initial orbit for tolerance calculation
     * @return configured AbstractIntegrator
     */
    AbstractIntegrator createIntegrator(IntegratorType integratorType, Orbit initialOrbit);
}
