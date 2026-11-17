
package dev.kayquemiguel.aegis.detection.persistence;

import dev.kayquemiguel.aegis.detection.DetectionAlert;
import dev.kayquemiguel.aegis.detection.AuthenticationDetectionAlert;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "detection_alerts")
public class DetectionAlertEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "rule_id", nullable = false, length = 64)
    private String ruleId;

    @Column(name = "sensor_id", nullable = false, length = 64)
    private String sensorId;

    @Column(name = "source_ip", nullable = false, length = 45)
    private String sourceIp;

    @Column(name = "destination_ip", length = 45)
    private String destinationIp;

    @Column(name = "auth_username", length = 256)
    private String authUsername;

    @Column(name = "auth_source_system", length = 128)
    private String authSourceSystem;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "distinct_ports", nullable = false)
    private int distinctPorts;

    @Column(name = "metric_type", nullable = false, length = 64)
    private String metricType;

    @Column(name = "observed_count", nullable = false)
    private int observedCount;

    @Column(name = "detection_threshold", nullable = false)
    private int detectionThreshold;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private Instant createdAt;

    protected DetectionAlertEntity() {
        // Required by JPA.
    }

    private DetectionAlertEntity(
            UUID id,
            DetectionAlert alert
    ) {
        this.id = Objects.requireNonNull(id);
        this.ruleId = alert.ruleId();
        this.sensorId = alert.sensorId();
        this.sourceIp = alert.sourceIp();
        this.destinationIp = alert.destinationIp();
        this.detectedAt = alert.detectedAt();
        this.distinctPorts = alert.distinctPorts();
        this.metricType = alert.metricType().name();
        this.observedCount = alert.observedCount();
        this.detectionThreshold = alert.threshold();
        this.description = alert.description();
    }

    public static DetectionAlertEntity from(
            DetectionAlert alert
    ) {
        Objects.requireNonNull(alert, "alert is required");

        return new DetectionAlertEntity(
                UUID.randomUUID(),
                alert
        );
    }

    public static DetectionAlertEntity from(
            AuthenticationDetectionAlert alert
    ) {
        Objects.requireNonNull(alert, "alert is required");

        if (!"NET-003".equals(alert.ruleId())) {
            throw new IllegalArgumentException(
                    "Expected NET-003 authentication alert"
            );
        }

        if (alert.observedCount() < 1
                || alert.threshold() < 1) {
            throw new IllegalArgumentException(
                    "Authentication counts must be positive"
            );
        }

        if (alert.username() == null
                || alert.username().isBlank()
                || alert.username().length() > 256
                || alert.sourceSystem() == null
                || alert.sourceSystem().isBlank()
                || alert.sourceSystem().length() > 128) {
            throw new IllegalArgumentException(
                    "Invalid authentication metadata"
            );
        }

        DetectionAlertEntity entity =
                new DetectionAlertEntity();

        entity.id = UUID.randomUUID();
        entity.ruleId = alert.ruleId();
        entity.sensorId = alert.sensorId();
        entity.sourceIp = alert.sourceIp();
        entity.destinationIp = null;
        entity.detectedAt = alert.detectedAt();
        entity.distinctPorts = 0;
        entity.metricType = "AUTHENTICATION_FAILURES";
        entity.observedCount = alert.observedCount();
        entity.detectionThreshold = alert.threshold();
        entity.description =
                "Repeated authentication failures";
        entity.authUsername = alert.username();
        entity.authSourceSystem = alert.sourceSystem();

        return entity;
    }

    public DetectionAlert toDomain() {
        if ("NET-003".equals(ruleId)) {
            throw new IllegalStateException(
                    "NET-003 requires authentication alert mapping"
            );
        }

        return new DetectionAlert(
                ruleId,
                sourceIp,
                destinationIp,
                detectedAt,
                distinctPorts,
                detectionThreshold,
                description,
                sensorId,
                dev.kayquemiguel.aegis.detection.DetectionMetricType
                        .valueOf(metricType)
        );
    }

    public AuthenticationDetectionAlert
    toAuthenticationDomain() {
        if (!"NET-003".equals(ruleId)) {
            throw new IllegalStateException(
                    "Not an authentication alert"
            );
        }

        return new AuthenticationDetectionAlert(
                ruleId,
                detectedAt,
                sensorId,
                authSourceSystem,
                sourceIp,
                authUsername,
                observedCount,
                detectionThreshold
        );
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public String getAuthSourceSystem() {
        return authSourceSystem;
    }

    public UUID getId() {
        return id;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getSensorId() {
        return sensorId;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getDestinationIp() {
        return destinationIp;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public int getDistinctPorts() {
        return distinctPorts;
    }

    public String getMetricType() {
        return metricType;
    }

    public int getObservedCount() {
        return observedCount;
    }

    public int getDetectionThreshold() {
        return detectionThreshold;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
