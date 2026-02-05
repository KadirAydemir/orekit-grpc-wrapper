package tr.com.kadiraydemir.orekit.model;

public record TLEPropagateRequest(
        PropagationModelType model,
        String tleLine1,
        String tleLine2,
        String startDate,
        String endDate,
        int positionCount,
        ReferenceFrameType outputFrame,
        IntegratorType integrator) {
}
