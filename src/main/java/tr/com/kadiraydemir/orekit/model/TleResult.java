package tr.com.kadiraydemir.orekit.model;

import java.util.List;

public record TleResult(
                List<PositionPointResult> positions,
                String frame) {
        public record PositionPointResult(
                        double x,
                        double y,
                        double z,
                        String timestamp) {
        }
}
