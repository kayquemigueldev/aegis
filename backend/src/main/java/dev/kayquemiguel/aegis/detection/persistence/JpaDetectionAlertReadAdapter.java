package dev.kayquemiguel.aegis.detection.persistence;

import dev.kayquemiguel.aegis.detection.application.DetectionAlertReadPort;
import dev.kayquemiguel.aegis.detection.application.DetectionAlertView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of the alert read port.
 */
@Repository
public class JpaDetectionAlertReadAdapter
        implements DetectionAlertReadPort {

    private final DetectionAlertRepository repository;

    public JpaDetectionAlertReadAdapter(
            DetectionAlertRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public Page<DetectionAlertView> findAll(
            Pageable pageable
    ) {
        return repository.findAll(pageable)
                .map(DetectionAlertViewMapper::from);
    }

    @Override
    public Page<DetectionAlertView> findBySensorId(
            String sensorId,
            Pageable pageable
    ) {
        return repository.findBySensorId(sensorId, pageable)
                .map(DetectionAlertViewMapper::from);
    }

    @Override
    public Page<DetectionAlertView> findByRuleId(
            String ruleId,
            Pageable pageable
    ) {
        return repository.findByRuleId(ruleId, pageable)
                .map(DetectionAlertViewMapper::from);
    }

    @Override
    public Page<DetectionAlertView> findBySensorIdAndRuleId(
            String sensorId,
            String ruleId,
            Pageable pageable
    ) {
        return repository.findBySensorIdAndRuleId(
                sensorId, ruleId, pageable
        ).map(DetectionAlertViewMapper::from);
    }

    @Override
    public Page<DetectionAlertView> findBySensorIdIn(
            Collection<String> sensorIds,
            Pageable pageable
    ) {
        return repository.findBySensorIdIn(sensorIds, pageable)
                .map(DetectionAlertViewMapper::from);
    }

    @Override
    public Page<DetectionAlertView> findBySensorIdInAndRuleId(
            Collection<String> sensorIds,
            String ruleId,
            Pageable pageable
    ) {
        return repository.findBySensorIdInAndRuleId(
                sensorIds, ruleId, pageable
        ).map(DetectionAlertViewMapper::from);
    }

    @Override
    public Optional<DetectionAlertView> findById(
            UUID id
    ) {
        return repository.findById(id)
                .map(DetectionAlertViewMapper::from);
    }
}
