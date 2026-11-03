
package dev.kayquemiguel.aegis.detection.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DetectionAlertRepository
        extends JpaRepository<DetectionAlertEntity, UUID> {

    // Consultas existentes: manter compatibilidade.
    List<DetectionAlertEntity>
    findBySensorIdOrderByDetectedAtDesc(String sensorId);

    List<DetectionAlertEntity>
    findByRuleIdOrderByDetectedAtDesc(String ruleId);

    // Novas consultas paginadas.
    Page<DetectionAlertEntity> findBySensorId(
            String sensorId,
            Pageable pageable
    );

    Page<DetectionAlertEntity> findByRuleId(
            String ruleId,
            Pageable pageable
    );

    Page<DetectionAlertEntity> findBySensorIdAndRuleId(
            String sensorId,
            String ruleId,
            Pageable pageable
    );
}
