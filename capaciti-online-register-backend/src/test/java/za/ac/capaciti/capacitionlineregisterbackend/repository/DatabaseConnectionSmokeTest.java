package za.ac.capaciti.capacitionlineregisterbackend.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import za.ac.capaciti.capacitionlineregisterbackend.domain.CampusHub;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

// Throwaway smoke test: reads the campus row from the real Supabase database.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DatabaseConnectionSmokeTest {

    @Autowired
    private CampusHubRepository campusHubRepository;

    @Test
    void canReadCampusHubFromSupabase() {
        List<CampusHub> campuses = campusHubRepository.findAll();
        assertFalse(campuses.isEmpty(), "No campus_hubs rows found");
        CampusHub campus = campuses.get(0);
        System.out.println("SMOKE TEST campus: " + campus.getName()
                + " | radius " + campus.getRadiusM()
                + " | cidrs " + campus.getWhitelistedCidrs().size());
    }
}
