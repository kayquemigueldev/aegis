
package dev.kayquemiguel.aegis.detection.persistence;

import dev.kayquemiguel.aegis.detection.DetectionAlert;
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

    @Column(name = "destination_ip", nullable = false, length = 45)
    private String destinationIp;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "distinct_ports", nullable = false)
    private int distinctPorts;

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

    public DetectionAlert toDomain() {
        return new DetectionAlert(
                ruleId,
                sourceIp,
                destinationIp,
                detectedAt,
                distinctPorts,
                detectionThreshold,
                description,
                sensorId
        );
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
