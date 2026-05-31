package com.berdachuk.medexpertmatch.core.rest;

import com.berdachuk.medexpertmatch.core.domain.ApiSessionTokenCreated;
import com.berdachuk.medexpertmatch.core.domain.ApiSessionTokenView;
import com.berdachuk.medexpertmatch.core.domain.AuditLog;
import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import com.berdachuk.medexpertmatch.core.security.AdminAccessGuard;
import com.berdachuk.medexpertmatch.core.service.ApiSessionTokenAdminService;
import com.berdachuk.medexpertmatch.core.service.ChatExportAuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Admin", description = "Simulated admin APIs (requires X-User-Id: admin)")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminAccessGuard adminAccessGuard;
    private final ApiSessionTokenAdminService sessionTokenAdminService;
    private final ChatExportAuditQueryService chatExportAuditQueryService;

    public AdminController(
            AdminAccessGuard adminAccessGuard,
            ApiSessionTokenAdminService sessionTokenAdminService,
            ChatExportAuditQueryService chatExportAuditQueryService) {
        this.adminAccessGuard = adminAccessGuard;
        this.sessionTokenAdminService = sessionTokenAdminService;
        this.chatExportAuditQueryService = chatExportAuditQueryService;
    }

    @Operation(summary = "List API session tokens (masked keys)")
    @GetMapping("/session-tokens")
    public List<ApiSessionTokenView> listSessionTokens() {
        adminAccessGuard.requireAdmin();
        return sessionTokenAdminService.listTokens();
    }

    @Operation(summary = "Create API session token (full key returned once)")
    @PostMapping("/session-tokens")
    public ApiSessionTokenCreated createSessionToken(@RequestBody Map<String, String> body) {
        adminAccessGuard.requireAdmin();
        RateLimitTier tier = RateLimitTier.fromDatabaseValue(body.get("rateLimitTier"));
        Instant expiresAt = body.containsKey("expiresAt") && body.get("expiresAt") != null
                ? Instant.parse(body.get("expiresAt"))
                : null;
        return sessionTokenAdminService.createToken(body.get("description"), tier, expiresAt);
    }

    @Operation(summary = "Revoke API session token")
    @DeleteMapping("/session-tokens/{id}")
    public ResponseEntity<Map<String, String>> revokeSessionToken(@PathVariable String id) {
        adminAccessGuard.requireAdmin();
        if (!sessionTokenAdminService.revokeToken(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "not_found"));
        }
        return ResponseEntity.ok(Map.of("status", "revoked"));
    }

    @Operation(summary = "List chat export audit events (hashed ids only)")
    @GetMapping("/audit/chat-exports")
    public List<Map<String, Object>> listChatExportAudits(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        adminAccessGuard.requireAdmin();
        return chatExportAuditQueryService.listChatExports(limit, offset).stream()
                .map(this::toAuditResponse)
                .toList();
    }

    private Map<String, Object> toAuditResponse(AuditLog log) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", log.id());
        row.put("action", log.action());
        row.put("resourceType", log.resourceType());
        row.put("resourceIdHash", log.resourceId());
        row.put("actorHash", log.actor());
        row.put("details", log.details());
        row.put("createdAt", log.createdAt() != null ? log.createdAt().toString() : null);
        return row;
    }
}
