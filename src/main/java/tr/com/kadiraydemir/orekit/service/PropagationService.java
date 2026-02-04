package tr.com.kadiraydemir.orekit.service;

import tr.com.kadiraydemir.orekit.grpc.PropagateRequest;
import tr.com.kadiraydemir.orekit.grpc.TLEPropagateRequest;
import tr.com.kadiraydemir.orekit.model.OrbitResult;
import tr.com.kadiraydemir.orekit.model.TleResult;

public interface PropagationService {
    OrbitResult propagate(PropagateRequest request);

    TleResult propagateTLE(TLEPropagateRequest request);
}
