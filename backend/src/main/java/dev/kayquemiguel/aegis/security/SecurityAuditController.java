
package dev.kayquemiguel.aegis.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit")
public class SecurityAuditController {

    private final SecurityAuditService service;

    public SecurityAuditController(
            SecurityAuditService service
    ) {
        this.service = service;
    }

    @GetMapping
    public SecurityAuditService.AuditPage list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action
    ) {
        return service.findEvents(
                page,
                size,
                actor,
                action
        );
    }
}
