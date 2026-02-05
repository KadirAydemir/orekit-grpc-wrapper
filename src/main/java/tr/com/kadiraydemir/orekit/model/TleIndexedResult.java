package tr.com.kadiraydemir.orekit.model;

import java.util.List;

public record TleIndexedResult(
    int tleIndex,
    List<TleResult.PositionPointResult> positions,
    String frame
) {}
