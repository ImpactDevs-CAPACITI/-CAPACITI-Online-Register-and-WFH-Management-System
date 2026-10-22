package za.ac.capaciti.capacitionlineregisterbackend.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.ac.capaciti.capacitionlineregisterbackend.domain.CampusHub;
import za.ac.capaciti.capacitionlineregisterbackend.domain.User;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.user.CreateUserRequest;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.user.UpdateUserRequest;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.user.UpdateUserStatusRequest;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.user.UserDto;
import za.ac.capaciti.capacitionlineregisterbackend.enums.UserRole;
import za.ac.capaciti.capacitionlineregisterbackend.exception.BadRequestException;
import za.ac.capaciti.capacitionlineregisterbackend.exception.ConflictException;
import za.ac.capaciti.capacitionlineregisterbackend.exception.ResourceNotFoundException;
import za.ac.capaciti.capacitionlineregisterbackend.mappers.UserMapper;
import za.ac.capaciti.capacitionlineregisterbackend.repository.CampusHubRepository;
import za.ac.capaciti.capacitionlineregisterbackend.repository.UserRepository;

@Service
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final CampusHubRepository campusHubRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final IAuditService auditService;

    public UserService(
            UserRepository userRepository,
            CampusHubRepository campusHubRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            IAuditService auditService) {
        this.userRepository = userRepository;
        this.campusHubRepository = campusHubRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public UserDto create(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "A user with that email already exists.");
        }

        String[] name = splitName(request.fullName());
        User user = new User();
        user.setEmail(request.email().trim());
        user.setFirstName(name[0]);
        user.setLastName(name[1]);
        user.setPasswordHash(passwordEncoder.encode(request.temporaryPassword()));
        user.setRole(request.role().name());
        user.setDefaultMode(request.defaultMode());
        user.setCampus(findCampus(request.campusId()));
        user.setChampion(findChampion(request.championId()));
        user.setActive(true);

        User saved = userRepository.save(user);
        logChange(saved, "USER_CREATED", Map.of("role", saved.getRole()));
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public UserDto update(UUID id, UpdateUserRequest request) {
        User user = findUser(id);
        String[] name = splitName(request.fullName());
        user.setFirstName(name[0]);
        user.setLastName(name[1]);
        user.setDefaultMode(request.defaultMode());
        user.setCampus(findCampus(request.campusId()));
        user.setChampion(findChampion(request.championId()));
        user.setRole(request.role().name());

        User saved = userRepository.save(user);
        logChange(saved, "USER_UPDATED", Map.of("role", saved.getRole()));
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public UserDto setActive(UUID id, UpdateUserStatusRequest request) {
        User user = findUser(id);
        user.setActive(request.active());
        User saved = userRepository.save(user);
        logChange(saved, request.active() ? "USER_ACTIVATED" : "USER_DEACTIVATED",
                Map.of("active", request.active()));
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> list(UserRole role, Boolean active, String text, Pageable pageable) {
        String searchText = text == null || text.isBlank() ? null : text.trim();
        return userRepository.searchUsers(role == null ? null : role.name(), active, searchText, pageable)
            .map(userMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getById(UUID id) {
        return userMapper.toDto(findUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrent(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return userMapper.toDto(user);
    }

    private CampusHub findCampus(UUID campusId) {
        if (campusId == null) {
            return null;
        }
        return campusHubRepository.findById(campusId)
                .orElseThrow(() -> new ResourceNotFoundException("Campus not found: " + campusId));
    }

    private User findChampion(UUID championId) {
        if (championId == null) {
            return null;
        }
        User champion = findUser(championId);
        if (!UserRole.TECH_CHAMP.name().equals(champion.getRole())) {
            throw new BadRequestException("The assigned champion must have the TECH_CHAMP role.");
        }
        return champion;
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private String[] splitName(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        return new String[] { parts[0], parts.length > 1 ? parts[1] : "" };
    }

    private void logChange(User changedUser, String action, Map<String, Object> details) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }
        userRepository.findByEmailIgnoreCase(authentication.getName())
                .ifPresent(actor -> auditService.log(actor, action, "User", changedUser.getId().toString(), details));
    }
}