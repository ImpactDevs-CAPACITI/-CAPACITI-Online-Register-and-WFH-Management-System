package za.ac.capaciti.capacitionlineregisterbackend.controller;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.attendance.AttendanceActionResponse;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.attendance.AttendanceDto;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.attendance.ClockInRequest;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.attendance.ClockOutRequest;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.attendance.ExcuseRequest;
import za.ac.capaciti.capacitionlineregisterbackend.enums.ArrivalStatus;
import za.ac.capaciti.capacitionlineregisterbackend.enums.WorkMode;
import za.ac.capaciti.capacitionlineregisterbackend.service.IAttendanceService;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    private final IAttendanceService attendanceService;

    @Autowired
    public AttendanceController(IAttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/clock-in")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<AttendanceActionResponse> clockIn(
            Authentication authentication,
            @Valid @RequestBody ClockInRequest request,
            HttpServletRequest http) {
        return new ResponseEntity<>(attendanceService.clockIn(authentication.getName(), request, http), HttpStatus.CREATED);
    }

    @PostMapping("/clock-out")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<AttendanceActionResponse> clockOut(
            Authentication authentication,
            @Valid @RequestBody ClockOutRequest request) {
        return new ResponseEntity<>(attendanceService.clockOut(authentication.getName(), request), HttpStatus.OK);
    }

    @GetMapping("/today")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<AttendanceDto> today(Authentication authentication) {
        return new ResponseEntity<>(attendanceService.today(authentication.getName()), HttpStatus.OK);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Page<AttendanceDto>> mine(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return new ResponseEntity<>(attendanceService.mine(authentication.getName(), PageRequest.of(page, size)),
                HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TECH_CHAMP','ADMIN')")
    public ResponseEntity<Page<AttendanceDto>> search(
            @RequestParam(required = false) UUID userId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) ArrivalStatus arrivalStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return new ResponseEntity<>(attendanceService.search(userId, from, to, workMode, arrivalStatus,
                PageRequest.of(page, size)), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECH_CHAMP','ADMIN')")
    public ResponseEntity<AttendanceDto> getById(@PathVariable UUID id) {
        return new ResponseEntity<>(attendanceService.getById(id), HttpStatus.OK);
    }

    @PatchMapping("/{id}/excuse")
    @PreAuthorize("hasAnyRole('TECH_CHAMP','ADMIN')")
    public ResponseEntity<AttendanceDto> excuse(
            @PathVariable UUID id,
            @Valid @RequestBody ExcuseRequest request,
            Authentication authentication) {
        return new ResponseEntity<>(attendanceService.excuse(id, request, authentication.getName()), HttpStatus.OK);
    }
}