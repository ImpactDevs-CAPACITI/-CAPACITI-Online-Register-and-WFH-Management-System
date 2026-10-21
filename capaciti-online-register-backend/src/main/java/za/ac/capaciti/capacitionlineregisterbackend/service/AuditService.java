package za.ac.capaciti.capacitionlineregisterbackend.service;

import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.capaciti.capacitionlineregisterbackend.domain.AuditLog;
import za.ac.capaciti.capacitionlineregisterbackend.domain.User;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.audit.AuditLogDto;
import za.ac.capaciti.capacitionlineregisterbackend.repository.AuditLogRepository;

@Service
public class AuditService implements IAuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void log(User actor, String action, String targetType, String targetId, Map<String, Object> details) {
        auditLogRepository.save(AuditLog.builder()
                .actor(actor)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> list(UUID actorId, String action, java.time.Instant from, java.time.Instant to,
                                  Pageable pageable) {
        return auditLogRepository.searchAuditLogs(actorId, action, from, to, pageable)
                .map(auditLog -> new AuditLogDto(
                        auditLog.getId(),
                        auditLog.getActor() == null ? null : auditLog.getActor().getId(),
                        auditLog.getActor() == null ? null : auditLog.getActor().getFullName(),
                        auditLog.getAction(),
                        auditLog.getTargetType(),
                        auditLog.getTargetId(),
                        auditLog.getDetails(),
                        auditLog.getCreatedAt()));
    }
}
