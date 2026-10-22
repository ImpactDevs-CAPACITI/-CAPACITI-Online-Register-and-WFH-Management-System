package za.ac.capaciti.capacitionlineregisterbackend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.ac.capaciti.capacitionlineregisterbackend.domain.AttendanceRecord;
import za.ac.capaciti.capacitionlineregisterbackend.domain.CampusHub;
import za.ac.capaciti.capacitionlineregisterbackend.domain.User;
import za.ac.capaciti.capacitionlineregisterbackend.domain.WfhRequest;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.attendance.AttendanceActionResponse;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.attendance.AttendanceDto;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.attendance.ClockInRequest;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.attendance.ClockOutRequest;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.attendance.ExcuseRequest;
import za.ac.capaciti.capacitionlineregisterbackend.domain.enums.WfhStatus;
import za.ac.capaciti.capacitionlineregisterbackend.enums.ArrivalStatus;
import za.ac.capaciti.capacitionlineregisterbackend.enums.DepartureStatus;
import za.ac.capaciti.capacitionlineregisterbackend.enums.WorkMode;
import za.ac.capaciti.capacitionlineregisterbackend.exception.BadRequestException;
import za.ac.capaciti.capacitionlineregisterbackend.exception.ConflictException;
import za.ac.capaciti.capacitionlineregisterbackend.exception.ResourceNotFoundException;
import za.ac.capaciti.capacitionlineregisterbackend.mappers.AttendanceMapper;
import za.ac.capaciti.capacitionlineregisterbackend.repository.AttendanceRecordRepository;
import za.ac.capaciti.capacitionlineregisterbackend.repository.CampusHubRepository;
import za.ac.capaciti.capacitionlineregisterbackend.repository.UserRepository;
import za.ac.capaciti.capacitionlineregisterbackend.repository.WfhRequestRepository;
import za.ac.capaciti.capacitionlineregisterbackend.util.TimeUtil;

@Service
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserRepository userRepository;
    private final WfhRequestRepository wfhRequestRepository;
    private final CampusHubRepository campusHubRepository;
    private final AttendanceMapper attendanceMapper;
    private final AttendanceStatusEngine attendanceStatusEngine;
    private final LocationVerificationService locationVerificationService;
    private final IIpWhitelistService ipWhitelistService;
    private final IAuditService auditService;
    private final String campusName;

    public AttendanceService(
            AttendanceRecordRepository attendanceRecordRepository,
            UserRepository userRepository,
            WfhRequestRepository wfhRequestRepository,
            CampusHubRepository campusHubRepository,
            AttendanceMapper attendanceMapper,
            AttendanceStatusEngine attendanceStatusEngine,
            LocationVerificationService locationVerificationService,
            IIpWhitelistService ipWhitelistService,
            IAuditService auditService,
            @Value("${app.seed.campus-name:CAPACITI Cape Town}") String campusName) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.userRepository = userRepository;
        this.wfhRequestRepository = wfhRequestRepository;
        this.campusHubRepository = campusHubRepository;
        this.attendanceMapper = attendanceMapper;
        this.attendanceStatusEngine = attendanceStatusEngine;
        this.locationVerificationService = locationVerificationService;
        this.ipWhitelistService = ipWhitelistService;
        this.auditService = auditService;
        this.campusName = campusName;
    }

    @Transactional
    public AttendanceActionResponse clockIn(String email, ClockInRequest request, HttpServletRequest http) {
        User user = findUser(email);
        LocalDate today = TimeUtil.today();
        if (attendanceRecordRepository.findByUserAndAttendanceDate(user, today).isPresent()) {
            throw new ConflictException("ALREADY_CLOCKED_IN", "Attendance has already been recorded for today.");
        }

        WfhRequest approvedWfh = wfhRequestRepository.findApprovedCoveringForUser(user, today).stream()
                .filter(wfhRequest -> wfhRequest.getStatus() == WfhStatus.APPROVED)
                .findFirst()
                .orElse(null);
        WorkMode workMode = approvedWfh == null ? WorkMode.ON_SITE : WorkMode.WFH;
        String clientIp = ipWhitelistService.extractClientIp(http);
        LocationResult location = workMode == WorkMode.ON_SITE
                ? verifyOnSiteLocation(request, clientIp)
                : new LocationResult(false, false, null, true, clientIp);

        Instant clockInAt = Instant.now();
        ArrivalStatus arrivalStatus = attendanceStatusEngine.arrivalStatusFor(clockInAt, false);
        AttendanceRecord record = AttendanceRecord.builder()
                .user(user)
                .attendanceDate(today)
                .workMode(workMode)
                .wfhRequest(approvedWfh)
                .clockInAt(clockInAt)
                .arrivalStatus(arrivalStatus)
                .clientIp(location.clientIp())
                .ipVerified(location.ipVerified())
                .geoVerified(location.geoVerified())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .accuracyM(request.accuracyM())
                .distanceM(location.distanceM())
                .build();
        AttendanceRecord saved = attendanceRecordRepository.save(record);

        Map<String, Object> details = new HashMap<>();
        details.put("workMode", workMode.name());
        details.put("arrivalStatus", arrivalStatus.name());
        details.put("attendanceDate", today.toString());
        auditService.log(user, "ATTENDANCE_CLOCKED_IN", "AttendanceRecord", saved.getId().toString(), details);

        boolean requiresReason = attendanceStatusEngine.requiresLateReason(arrivalStatus);
        String feedback = requiresReason
                ? "Clock-in recorded. A reason is required for your late arrival."
                : "Clock-in recorded.";
        return new AttendanceActionResponse(attendanceMapper.toDto(saved), requiresReason, feedback,
                saved.isAiQualityFlag());
    }

    @Transactional
    public AttendanceActionResponse clockOut(String email, ClockOutRequest request) {
        User user = findUser(email);
        AttendanceRecord record = findTodayRecord(user);
        if (record.getClockInAt() == null) {
            throw new ConflictException("NOT_CLOCKED_IN", "You must clock in before clocking out.");
        }
        if (record.getClockOutAt() != null) {
            throw new ConflictException("ALREADY_CLOCKED_OUT", "You have already clocked out today.");
        }

        Instant clockOutAt = Instant.now();
        DepartureStatus departureStatus = attendanceStatusEngine.departureStatusFor(clockOutAt);
        if (departureStatus == DepartureStatus.PARTIAL_DAY
                && (request.earlyClockoutReason() == null || request.earlyClockoutReason().isBlank())) {
            throw new BadRequestException("A reason is required when clocking out before 16:00.");
        }
        if (departureStatus == DepartureStatus.LATE_CLOCKOUT
                && (request.lateClockoutNote() == null || request.lateClockoutNote().isBlank())) {
            throw new BadRequestException("A note is required when clocking out after 16:20.");
        }

        record.setClockOutAt(clockOutAt);
        record.setDepartureStatus(departureStatus);
        record.setEarlyClockoutReason(request.earlyClockoutReason());
        record.setLateClockoutNote(request.lateClockoutNote());
        record.setDeliverableLink(request.deliverableLink());
        AttendanceRecord saved = attendanceRecordRepository.save(record);
        auditService.log(user, "ATTENDANCE_CLOCKED_OUT", "AttendanceRecord", saved.getId().toString(), Map.of(
                "departureStatus", departureStatus.name(),
                "attendanceDate", saved.getAttendanceDate().toString()));

        boolean requiresReason = attendanceStatusEngine.requiresClockOutText(departureStatus);
        return new AttendanceActionResponse(attendanceMapper.toDto(saved), requiresReason,
                "Clock-out recorded.", saved.isAiQualityFlag());
    }

    @Transactional(readOnly = true)
    public AttendanceDto today(String email) {
        User user = findUser(email);
        return attendanceRecordRepository.findByUserAndAttendanceDate(user, TimeUtil.today())
                .map(attendanceMapper::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceDto> mine(String email, Pageable pageable) {
        User user = findUser(email);
        return attendanceRecordRepository.findByUserOrderByAttendanceDateDesc(user, pageable)
                .map(attendanceMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceDto> search(UUID userId, LocalDate from, LocalDate to, WorkMode workMode,
            ArrivalStatus arrivalStatus, Pageable pageable) {
        return attendanceRecordRepository.searchAttendanceRecords(userId, from, to, workMode, arrivalStatus, pageable)
                .map(attendanceMapper::toDto);
    }

    @Transactional(readOnly = true)
    public AttendanceDto getById(UUID id) {
        return attendanceRecordRepository.findById(id)
                .map(attendanceMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + id));
    }

    @Transactional
    public AttendanceDto excuse(UUID id, ExcuseRequest request, String actorEmail) {
        User actor = findUser(actorEmail);
        AttendanceRecord record = attendanceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + id));
        if (record.getArrivalStatus() != ArrivalStatus.LATE_UNEXCUSED) {
            throw new ConflictException("ATTENDANCE_NOT_EXCUSABLE", "Only unexcused late arrivals can be excused.");
        }

        record.setArrivalStatus(ArrivalStatus.LATE_EXCUSED);
        record.setLateReason(request.note().trim());
        AttendanceRecord saved = attendanceRecordRepository.save(record);
        auditService.log(actor, "ATTENDANCE_EXCUSED", "AttendanceRecord", saved.getId().toString(), Map.of(
                "userId", saved.getUser().getId().toString(),
                "note", request.note().trim()));
        return attendanceMapper.toDto(saved);
    }

    private LocationResult verifyOnSiteLocation(ClockInRequest request, String clientIp) {
        CampusHub campus = campusHubRepository.findByName(campusName)
                .orElseThrow(() -> new ResourceNotFoundException("Campus not found: " + campusName));
        LocationResult result = locationVerificationService.verify(
                campus, clientIp, request.latitude(), request.longitude(), request.accuracyM());
        if (!result.verified()) {
            throw new BadRequestException("Your location could not be verified for on-site attendance.");
        }
        return result;
    }

    private AttendanceRecord findTodayRecord(User user) {
        return attendanceRecordRepository.findByUserAndAttendanceDate(user, TimeUtil.today())
                .orElseThrow(() -> new ResourceNotFoundException("No attendance record found for today."));
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}