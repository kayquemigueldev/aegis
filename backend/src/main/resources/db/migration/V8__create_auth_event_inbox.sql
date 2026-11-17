CREATE TABLE auth_event_inbox (
    sensor_id VARCHAR(64) NOT NULL,
    event_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL,
    source_ip VARCHAR(45) NOT NULL,
    username VARCHAR(256) NOT NULL,
    source_system VARCHAR(128) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    alert_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_auth_event_inbox
        PRIMARY KEY (sensor_id, event_id),

    CONSTRAINT chk_auth_event_outcome
        CHECK (outcome IN ('SUCCESS', 'FAILURE')),

    CONSTRAINT chk_auth_event_username
        CHECK (LENGTH(BTRIM(username)) > 0),

    CONSTRAINT chk_auth_event_source_system
        CHECK (LENGTH(BTRIM(source_system)) > 0),

    CONSTRAINT chk_auth_event_processed
        CHECK (processed OR alert_id IS NULL),

    CONSTRAINT fk_auth_event_alert
        FOREIGN KEY (alert_id)
        REFERENCES detection_alerts(id)
);

CREATE INDEX idx_auth_event_inbox_pending
    ON auth_event_inbox (ingested_at, sensor_id, event_id)
    WHERE processed = FALSE;
