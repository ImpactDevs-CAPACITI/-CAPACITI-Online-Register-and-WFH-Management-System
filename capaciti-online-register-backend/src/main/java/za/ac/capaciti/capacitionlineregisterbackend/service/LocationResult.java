package za.ac.capaciti.capacitionlineregisterbackend.service;

public record LocationResult(
        boolean ipVerified,
        boolean geoVerified,
        Double distanceM,
        boolean verified,
        String clientIp) {
}
