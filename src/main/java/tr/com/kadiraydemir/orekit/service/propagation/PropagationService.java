package tr.com.kadiraydemir.orekit.service.propagation;

import tr.com.kadiraydemir.orekit.model.OrbitResult;
import tr.com.kadiraydemir.orekit.model.PropagateRequest;
import tr.com.kadiraydemir.orekit.model.PropagateTLEListRequest;
import tr.com.kadiraydemir.orekit.model.TLEPropagateRequest;
import tr.com.kadiraydemir.orekit.model.TleIndexedResult;
import tr.com.kadiraydemir.orekit.model.TleResult;

import io.smallrye.mutiny.Multi;

public interface PropagationService {
    OrbitResult propagate(PropagateRequest request);

    Multi<TleResult> propagateTLE(TLEPropagateRequest request);

    Multi<TleIndexedResult> propagateTLEList(PropagateTLEListRequest request);
}
