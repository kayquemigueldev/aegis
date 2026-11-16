ALTER TABLE detection_alerts
    ADD COLUMN metric_type VARCHAR(64)
        NOT NULL DEFAULT 'DISTINCT_DESTINATION_PORTS';

ALTER TABLE detection_alerts
    ADD COLUMN observed_count INTEGER;

UPDATE detection_alerts
SET observed_count = distinct_ports;

ALTER TABLE detection_alerts
    ALTER COLUMN observed_count SET NOT NULL;

ALTER TABLE detection_alerts
    ADD CONSTRAINT chk_detection_alerts_observed_count
        CHECK (observed_count > 0);

ALTER TABLE detection_alerts
    ADD CONSTRAINT chk_detection_alerts_metric_type
        CHECK (
            metric_type IN (
                'DISTINCT_DESTINATION_PORTS',
                'DISTINCT_DESTINATION_HOSTS'
            )
        );
