package za.ac.capaciti.capacitionlineregisterbackend.service;

import org.springframework.stereotype.Service;
import za.ac.capaciti.capacitionlineregisterbackend.domain.CampusHub;

@Service
public class LocationVerificationService {

    // TODO lead: STUB, replace with real IP and geofence check before the demo
    public LocationResult verify(CampusHub campus, String clientIp, Double lat, Double lon, Double accuracyM) {
        return new LocationResult(true, true, null, true, clientIp);
    }
}
