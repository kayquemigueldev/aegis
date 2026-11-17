-- AEGIS-099: reuse the existing detection_alerts table.

ALTER TABLE detection_alerts
    ADD COLUMN auth_username VARCHAR(256),
    ADD COLUMN auth_source_system VARCHAR(128);

-- Authentication failures do not necessarily have a
-- network destination IP.
ALTER TABLE detection_alerts
    ALTER COLUMN destination_ip DROP NOT NULL;

-- The legacy metric remains positive for NET-001/002.
-- NET-003 has no distinct-port count.
ALTER TABLE detection_alerts
    DROP CONSTRAINT chk_detection_alerts_ports;

ALTER TABLE detection_alerts
    ADD CONSTRAINT chk_detection_alerts_ports
    CHECK (
        (rule_id = 'NET-003' AND distinct_ports = 0)
        OR
        (rule_id <> 'NET-003' AND distinct_ports > 0)
    );

ALTER TABLE detection_alerts
    DROP CONSTRAINT chk_detection_alerts_metric_type;

ALTER TABLE detection_alerts
    ADD CONSTRAINT chk_detection_alerts_metric_type
    CHECK (
        metric_type IN (
            'DISTINCT_DESTINATION_PORTS',
            'DISTINCT_DESTINATION_HOSTS',
            'AUTHENTICATION_FAILURES'
        )
    );

-- Prevent missing authentication metadata and
-- preserve the existing network alert semantics.
ALTER TABLE detection_alerts
    ADD CONSTRAINT chk_detection_alerts_payload
    CHECK (
        (
            rule_id = 'NET-003'
            AND destination_ip IS NULL
            AND auth_username IS NOT NULL
            AND LENGTH(BTRIM(auth_username)) > 0
            AND auth_source_system IS NOT NULL
            AND LENGTH(BTRIM(auth_source_system)) > 0
            AND metric_type = 'AUTHENTICATION_FAILURES'
        )
        OR
        (
            rule_id <> 'NET-003'
            AND destination_ip IS NOT NULL
            AND auth_username IS NULL
            AND auth_source_system IS NULL
            AND metric_type IN (
                'DISTINCT_DESTINATION_PORTS',
                'DISTINCT_DESTINATION_HOSTS'
            )
        )
    );

CREATE INDEX idx_detection_alerts_auth
    ON detection_alerts (
        sensor_id,
        auth_username,
        detected_at DESC
    )
    WHERE rule_id = 'NET-003';
