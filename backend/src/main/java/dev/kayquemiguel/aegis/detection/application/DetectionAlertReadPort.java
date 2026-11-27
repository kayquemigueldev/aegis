package dev.kayquemiguel.aegis.detection.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Read operations required by alert query use cases.
 *
 * Implementations may use JPA, but callers do not
 * depend on persistence entities or repositories.
 */
public interface DetectionAlertReadPort {

    Page<DetectionAlertView> findAll(
            Pageable pageable
    );

    Page<DetectionAlertView> findBySensorId(
            String sensorId,
            Pageable pageable
    );

    Page<DetectionAlertView> findByRuleId(
            String ruleId,
            Pageable pageable
    );

    Page<DetectionAlertView> findBySensorIdAndRuleId(
            String sensorId,
            String ruleId,
            Pageable pageable
    );

    Page<DetectionAlertView> findBySensorIdIn(
            Collection<String> sensorIds,
            Pageable pageable
    );

    Page<DetectionAlertView> findBySensorIdInAndRuleId(
            Collection<String> sensorIds,
            String ruleId,
            Pageable pageable
    );

    Optional<DetectionAlertView> findById(
            UUID id
    );
}
