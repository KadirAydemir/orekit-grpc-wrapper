package tr.com.kadiraydemir.orekit.service.propagation;

import tr.com.kadiraydemir.orekit.grpc.PropagateRequest;
import tr.com.kadiraydemir.orekit.grpc.TLEPropagateRequest;
import tr.com.kadiraydemir.orekit.grpc.TLEListPropagateRequest;
import tr.com.kadiraydemir.orekit.model.OrbitResult;
import tr.com.kadiraydemir.orekit.model.TleResult;
import tr.com.kadiraydemir.orekit.model.TleIndexedResult;

import io.smallrye.mutiny.Multi;
import java.util.function.Consumer;

public interface PropagationService {
    OrbitResult propagate(PropagateRequest request);

    Multi<TleResult> propagateTLE(TLEPropagateRequest request);

    void propagateTLEBlocking(TLEPropagateRequest request, Consumer<TleResult> consumer);

    void propagateTLEListBlocking(TLEListPropagateRequest request, Consumer<TleIndexedResult> consumer);
}
