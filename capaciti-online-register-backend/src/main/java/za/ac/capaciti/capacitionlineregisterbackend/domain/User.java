package za.ac.capaciti.capacitionlineregisterbackend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.ac.capaciti.capacitionlineregisterbackend.enums.WorkMode;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "role", nullable = false)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_mode", nullable = false)
    private WorkMode defaultMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private CampusHub campus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "champion_id")
    private User champion;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String getFullName() {
        return (firstName + " " + lastName).trim();
    }
}
