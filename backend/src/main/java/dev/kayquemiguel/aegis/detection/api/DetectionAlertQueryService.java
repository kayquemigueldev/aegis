
package dev.kayquemiguel.aegis.detection.api;

import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertEntity;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class DetectionAlertQueryService {

    private final DetectionAlertRepository repository;

    public DetectionAlertQueryService(
            DetectionAlertRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public DetectionAlertPageResponse search(
            String sensorId,
            String ruleId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("detectedAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<DetectionAlertEntity> result;

        if (sensorId != null && ruleId != null) {
            result = repository.findBySensorIdAndRuleId(
                    sensorId,
                    ruleId,
                    pageable
            );
        } else if (sensorId != null) {
            result = repository.findBySensorId(
                    sensorId,
                    pageable
            );
        } else if (ruleId != null) {
            result = repository.findByRuleId(
                    ruleId,
                    pageable
            );
        } else {
            result = repository.findAll(pageable);
        }

        Page<DetectionAlertResponse> responses =
                result.map(DetectionAlertResponse::from);

        return DetectionAlertPageResponse.from(responses);
    }

    @Transactional(readOnly = true)
    public Optional<DetectionAlertResponse> findById(
            UUID id
    ) {
        return repository.findById(id)
                .map(DetectionAlertResponse::from);
    }
}
