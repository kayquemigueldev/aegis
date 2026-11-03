
package dev.kayquemiguel.aegis.detection.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DetectionAlertRepository
        extends JpaRepository<DetectionAlertEntity, UUID> {

    List<DetectionAlertEntity>
    findBySensorIdOrderByDetectedAtDesc(String sensorId);

    List<DetectionAlertEntity>
    findByRuleIdOrderByDetectedAtDesc(String ruleId);
}
