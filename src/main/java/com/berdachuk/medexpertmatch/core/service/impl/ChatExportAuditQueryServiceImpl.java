package com.berdachuk.medexpertmatch.core.service.impl;

import com.berdachuk.medexpertmatch.core.domain.AuditLog;
import com.berdachuk.medexpertmatch.core.repository.AuditLogRepository;
import com.berdachuk.medexpertmatch.core.service.ChatExportAuditQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatExportAuditQueryServiceImpl implements ChatExportAuditQueryService {

    static final String CHAT_EXPORT_ACTION = "CHAT_EXPORT";

    private final AuditLogRepository auditLogRepository;

    public ChatExportAuditQueryServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> listChatExports(int limit, int offset) {
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        int safeOffset = Math.max(offset, 0);
        return auditLogRepository.findByAction(CHAT_EXPORT_ACTION, safeLimit, safeOffset);
    }
}
