package tr.com.kadiraydemir.orekit.model;

public record OrbitResult(
        String satelliteName,
        double posX,
        double posY,
        double posZ,
        double velX,
        double velY,
        double velZ,
        String finalDateIso,
        String frameName) {
}
