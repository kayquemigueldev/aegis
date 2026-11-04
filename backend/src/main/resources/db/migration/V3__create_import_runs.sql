
CREATE TABLE import_runs (
                             id UUID PRIMARY KEY,

                             sensor_id VARCHAR(64) NOT NULL,
                             file_sha256 VARCHAR(64) NOT NULL,

                             total_lines BIGINT NOT NULL DEFAULT 0,
                             accepted_events BIGINT NOT NULL DEFAULT 0,
                             ignored_events BIGINT NOT NULL DEFAULT 0,
                             rejected_events BIGINT NOT NULL DEFAULT 0,
                             persisted_alerts BIGINT NOT NULL DEFAULT 0,

                             created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             completed_at TIMESTAMPTZ,

                             CONSTRAINT uq_import_runs_sensor_hash
                                 UNIQUE (sensor_id, file_sha256),

                             CONSTRAINT chk_import_runs_counters
                                 CHECK (
                                     total_lines >= 0
                                         AND accepted_events >= 0
                                         AND ignored_events >= 0
                                         AND rejected_events >= 0
                                         AND persisted_alerts >= 0
                                     ),

                             CONSTRAINT chk_import_runs_hash
                                 CHECK (file_sha256 ~ '^[0-9a-f]{64}$')
    );

CREATE INDEX idx_import_runs_sensor_created
    ON import_runs (sensor_id, created_at DESC);
