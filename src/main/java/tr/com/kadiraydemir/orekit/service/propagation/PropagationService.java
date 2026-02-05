package tr.com.kadiraydemir.orekit.service.propagation;

import tr.com.kadiraydemir.orekit.model.OrbitResult;
import tr.com.kadiraydemir.orekit.model.PropagateRequestDTO;
import tr.com.kadiraydemir.orekit.model.TLEPropagateRequestDTO;
import tr.com.kadiraydemir.orekit.model.TleResult;

import io.smallrye.mutiny.Multi;

public interface PropagationService {
    OrbitResult propagate(PropagateRequestDTO request);

    Multi<TleResult> propagateTLE(TLEPropagateRequestDTO request);
}
