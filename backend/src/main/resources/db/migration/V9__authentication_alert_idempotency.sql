-- AEGIS-101: NET-003 trigger event identity.
ALTER TABLE detection_alerts
    ADD COLUMN trigger_event_id UUID;

-- Historical alerts remain compatible.
CREATE UNIQUE INDEX ux_net003_trigger_event
    ON detection_alerts(sensor_id, trigger_event_id)
    WHERE rule_id = 'NET-003'
      AND trigger_event_id IS NOT NULL;
