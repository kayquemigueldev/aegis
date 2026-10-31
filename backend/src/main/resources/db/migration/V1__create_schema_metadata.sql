CREATE TABLE schema_metadata (
    id INTEGER PRIMARY KEY,
    application_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO schema_metadata (id, application_name)
VALUES (1, 'AEGIS');
