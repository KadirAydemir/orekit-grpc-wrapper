package tr.com.kadiraydemir.orekit.service.propagation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hipparchus.ode.AbstractIntegrator;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.DeepSDP4;
import org.orekit.propagation.analytical.tle.SGP4;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import tr.com.kadiraydemir.orekit.grpc.IntegratorType;
import tr.com.kadiraydemir.orekit.grpc.PropagationModel;

/**
 * Implementation of PropagatorFactoryService for creating propagators
 */
@ApplicationScoped
public class PropagatorFactoryServiceImpl implements PropagatorFactoryService {

    private static final int GRAVITY_DEGREE = 10;
    private static final int GRAVITY_ORDER = 10;
    private static final double SPACECRAFT_MASS = 1.0;

    private final IntegratorService integratorService;

    @Inject
    public PropagatorFactoryServiceImpl(IntegratorService integratorService) {
        this.integratorService = integratorService;
    }

    @Override
    public Propagator createPropagator(TLE tle, PropagationModel model, IntegratorType integratorType,
            Frame temeFrame) {
        if (model == null) {
            model = PropagationModel.AUTO;
        }

        return switch (model) {
            case SGP4 -> new SGP4(tle, new FrameAlignedProvider(temeFrame), SPACECRAFT_MASS);
            case SDP4 -> new DeepSDP4(tle, new FrameAlignedProvider(temeFrame), SPACECRAFT_MASS);
            case NUMERICAL -> createNumericalPropagator(tle, integratorType, temeFrame);
            default -> TLEPropagator.selectExtrapolator(tle); // AUTO - based on TLE period
        };
    }

    @Override
    public Propagator createNumericalPropagator(TLE tle, IntegratorType integratorType, Frame temeFrame) {
        // Use analytical propagator to get initial state at TLE epoch
        TLEPropagator analyticalPropagator = TLEPropagator.selectExtrapolator(tle);
        PVCoordinates initialPV = analyticalPropagator.getPVCoordinates(tle.getDate(), temeFrame);

        // Create orbit from PV coordinates
        Orbit initialOrbit = new org.orekit.orbits.CartesianOrbit(initialPV, temeFrame, tle.getDate(),
                Constants.WGS84_EARTH_MU);

        // Configure integrator based on user selection
        AbstractIntegrator integrator = integratorService.createIntegrator(integratorType, initialOrbit);

        // Create numerical propagator
        NumericalPropagator numProp = new NumericalPropagator(integrator);
        numProp.setOrbitType(OrbitType.CARTESIAN);

        // Add gravity force model
        NormalizedSphericalHarmonicsProvider gravityProvider = GravityFieldFactory.getNormalizedProvider(GRAVITY_DEGREE,
                GRAVITY_ORDER);
        numProp.addForceModel(new HolmesFeatherstoneAttractionModel(
                FramesFactory.getITRF(IERSConventions.IERS_2010, true), gravityProvider));

        numProp.setInitialState(new SpacecraftState(initialOrbit));

        return numProp;
    }
}
