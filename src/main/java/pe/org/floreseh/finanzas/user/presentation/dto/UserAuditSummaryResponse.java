package pe.org.floreseh.finanzas.user.presentation.dto;

import pe.org.floreseh.finanzas.user.domain.Role;

import java.util.List;
import java.util.Map;

public record UserAuditSummaryResponse(
        long totalActiveUsers,
        Map<Role, Long> countByRole,
        List<List<String>> auditBatches
) {
}
