package tr.com.kadiraydemir.orekit.model;

import java.util.List;

public record PropagateTLEListRequest(
    PropagationModelType model,
    List<TleData> tles,
    String startDate,
    String endDate,
    int positionCount,
    ReferenceFrameType outputFrame,
    IntegratorType integrator
) {}
