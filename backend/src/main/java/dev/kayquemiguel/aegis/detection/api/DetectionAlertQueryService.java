
package dev.kayquemiguel.aegis.detection.api;

import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertEntity;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertRepository;
import dev.kayquemiguel.aegis.security.SensorAccessPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class DetectionAlertQueryService {

    private final DetectionAlertRepository repository;
    private final SensorAccessPolicy accessPolicy;

    public DetectionAlertQueryService(
            DetectionAlertRepository repository,
            SensorAccessPolicy accessPolicy
    ) {
        this.repository = repository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public DetectionAlertPageResponse search(
            String sensorId,
            String ruleId,
            int page,
            int size,
            Authentication authentication
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

        if (sensorId != null) {
            // Nao basta confiar no filtro fornecido
            // pelo cliente. Ele deve estar autorizado.
            accessPolicy.requireRead(
                    authentication,
                    sensorId
            );

            if (ruleId != null) {
                result = repository.findBySensorIdAndRuleId(
                        sensorId,
                        ruleId,
                        pageable
                );
            } else {
                result = repository.findBySensorId(
                        sensorId,
                        pageable
                );
            }

        } else if (accessPolicy.isAdmin(authentication)) {
            // Somente ADMIN pode consultar globalmente.
            if (ruleId != null) {
                result = repository.findByRuleId(
                        ruleId,
                        pageable
                );
            } else {
                result = repository.findAll(pageable);
            }

        } else {
            // Analistas sem sensor explicito visualizam
            // apenas os sensores autorizados.
            Set<String> permitted =
                    accessPolicy.permittedSensors(
                            authentication
                    );

            if (permitted.isEmpty()) {
                result = Page.empty(pageable);
            } else if (ruleId != null) {
                result =
                        repository.findBySensorIdInAndRuleId(
                                permitted,
                                ruleId,
                                pageable
                        );
            } else {
                result = repository.findBySensorIdIn(
                        permitted,
                        pageable
                );
            }
        }

        Page<DetectionAlertResponse> responses =
                result.map(
                        DetectionAlertResponse::from
                );

        return DetectionAlertPageResponse.from(
                responses
        );
    }

    @Transactional(readOnly = true)
    public Optional<DetectionAlertResponse> findById(
            UUID id,
            Authentication authentication
    ) {
        return repository.findById(id)
                .filter(entity ->
                        accessPolicy.canRead(
                                authentication,
                                entity.getSensorId()
                        )
                )
                .map(DetectionAlertResponse::from);
    }
}
