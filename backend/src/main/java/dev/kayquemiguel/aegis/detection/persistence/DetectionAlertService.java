
package dev.kayquemiguel.aegis.detection.persistence;

import dev.kayquemiguel.aegis.detection.DetectionAlert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DetectionAlertService {

    private final DetectionAlertRepository repository;

    public DetectionAlertService(
            DetectionAlertRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public UUID save(DetectionAlert alert) {
        Objects.requireNonNull(alert, "alert is required");

        DetectionAlertEntity entity =
                DetectionAlertEntity.from(alert);

        return repository.save(entity).getId();
    }

    @Transactional(readOnly = true)
    public List<DetectionAlert> findBySensor(
            String sensorId
    ) {
        if (sensorId == null || sensorId.isBlank()) {
            throw new IllegalArgumentException(
                    "sensorId is required"
            );
        }

        return repository
                .findBySensorIdOrderByDetectedAtDesc(
                        sensorId.trim()
                )
                .stream()
                .map(DetectionAlertEntity::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DetectionAlert> findByRule(
            String ruleId
    ) {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException(
                    "ruleId is required"
            );
        }

        return repository
                .findByRuleIdOrderByDetectedAtDesc(
                        ruleId.trim()
                )
                .stream()
                .map(DetectionAlertEntity::toDomain)
                .toList();
    }
}
