-- Supports bounded replay of processed and pending
-- authentication events in deterministic ingestion order.
CREATE INDEX idx_auth_event_inbox_replay
    ON auth_event_inbox (
        ingested_at,
        sensor_id,
        event_id
    );
