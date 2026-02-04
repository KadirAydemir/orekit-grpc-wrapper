package tr.com.kadiraydemir.orekit.model;

import java.util.List;

public record TleIndexedResult(
        int index,
        List<TleResult.PositionPointResult> positions,
        String frame) {
}
