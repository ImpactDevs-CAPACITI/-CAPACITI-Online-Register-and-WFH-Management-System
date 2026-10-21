package za.ac.capaciti.capacitionlineregisterbackend.controller;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.campus.CampusDto;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.campus.UpdateCampusRequest;
import za.ac.capaciti.capacitionlineregisterbackend.service.CampusService;

@RestController
@RequestMapping("/campuses")
public class CampusController {

    private final CampusService campusService;

    @Autowired
    public CampusController(CampusService campusService) {
        this.campusService = campusService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CampusDto>> list() {
        return new ResponseEntity<>(campusService.list(), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampusDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCampusRequest request,
            Authentication authentication) {
        return new ResponseEntity<>(campusService.update(id, request, authentication.getName()), HttpStatus.OK);
    }

    @PostMapping("/{id}/whitelist/current-ip")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampusDto> addCurrentIp(
            @PathVariable UUID id,
            HttpServletRequest request,
            Authentication authentication) {
        return new ResponseEntity<>(campusService.addCurrentIp(id, request, authentication.getName()), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}/whitelist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampusDto> removeCidr(
            @PathVariable UUID id,
            @RequestParam String cidr,
            Authentication authentication) {
        return new ResponseEntity<>(campusService.removeCidr(id, cidr, authentication.getName()), HttpStatus.OK);
    }
}