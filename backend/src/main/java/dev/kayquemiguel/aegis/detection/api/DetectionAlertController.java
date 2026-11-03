
package dev.kayquemiguel.aegis.detection.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class DetectionAlertController {

    private static final int MAX_PAGE_SIZE = 100;

    private final DetectionAlertQueryService queryService;

    public DetectionAlertController(
            DetectionAlertQueryService queryService
    ) {
        this.queryService = queryService;
    }

    @GetMapping
    public DetectionAlertPageResponse findAll(
            @RequestParam(required = false) String sensorId,
            @RequestParam(required = false) String ruleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0) {
            throw badRequest("page must be >= 0");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw badRequest(
                    "size must be between 1 and 100"
            );
        }

        String validatedSensor = validateFilter(
                sensorId,
                "sensorId"
        );

        String validatedRule = validateFilter(
                ruleId,
                "ruleId"
        );

        return queryService.search(
                validatedSensor,
                validatedRule,
                page,
                size
        );
    }

    @GetMapping("/{id}")
    public DetectionAlertResponse findById(
            @PathVariable UUID id
    ) {
        return queryService.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Detection alert not found"
                        )
                );
    }

    private static String validateFilter(
            String value,
            String field
    ) {
        if (value == null) {
            return null;
        }

        if (value.isBlank()) {
            throw badRequest(field + " must not be blank");
        }

        String normalized = value.trim();

        if (!normalized.matches(
                "[A-Za-z0-9][A-Za-z0-9._-]{0,63}"
        )) {
            throw badRequest(
                    field + " contains invalid characters"
            );
        }

        return normalized;
    }

    private static ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
