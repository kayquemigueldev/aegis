
CREATE TABLE detection_alerts (
                                  id UUID PRIMARY KEY,
                                  rule_id VARCHAR(64) NOT NULL,
                                  sensor_id VARCHAR(64) NOT NULL,
                                  source_ip VARCHAR(45) NOT NULL,
                                  destination_ip VARCHAR(45) NOT NULL,
                                  detected_at TIMESTAMPTZ NOT NULL,
                                  distinct_ports INTEGER NOT NULL,
                                  detection_threshold INTEGER NOT NULL,
                                  description VARCHAR(1000) NOT NULL,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT chk_detection_alerts_ports
                                      CHECK (distinct_ports > 0),

                                  CONSTRAINT chk_detection_alerts_threshold
                                      CHECK (detection_threshold > 0)
);

CREATE INDEX idx_detection_alerts_detected_at
    ON detection_alerts (detected_at DESC);

CREATE INDEX idx_detection_alerts_sensor_detected
    ON detection_alerts (sensor_id, detected_at DESC);

CREATE INDEX idx_detection_alerts_rule_detected
    ON detection_alerts (rule_id, detected_at DESC);
