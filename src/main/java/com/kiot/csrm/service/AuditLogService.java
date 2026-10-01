package com.kiot.csrm.service;

import com.kiot.csrm.entity.AuditLog;
import com.kiot.csrm.entity.User;
import com.kiot.csrm.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logAction(User user, String action, String details) {
        AuditLog auditLog = new AuditLog(user, action, details);
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getFilteredLogs(Long userId, String action, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findFilteredAuditLogs(userId, action, startDate, endDate);
    }
}
