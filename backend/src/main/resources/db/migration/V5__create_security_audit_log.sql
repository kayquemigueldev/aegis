
CREATE TABLE aegis_security_audit_log (
                                          id UUID PRIMARY KEY,

                                          actor_username VARCHAR(64) NOT NULL,

                                          action VARCHAR(40) NOT NULL,

                                          target_user_id UUID,

                                          target_username VARCHAR(64),

                                          details VARCHAR(255),

                                          occurred_at TIMESTAMPTZ NOT NULL
                                              DEFAULT CURRENT_TIMESTAMP,

                                          CONSTRAINT chk_aegis_audit_action
                                              CHECK (
                                                  action IN (
                                                  'USER_CREATED',
                                                  'USER_SENSORS_UPDATED',
                                                  'USER_STATUS_UPDATED',
                                                  'ADMIN_PASSWORD_CHANGED'
                                                  )
)
    );

CREATE INDEX idx_aegis_audit_occurred
    ON aegis_security_audit_log (
                                 occurred_at DESC,
                                 id DESC
        );

CREATE INDEX idx_aegis_audit_actor
    ON aegis_security_audit_log (
                                 actor_username,
                                 occurred_at DESC
        );

CREATE INDEX idx_aegis_audit_action
    ON aegis_security_audit_log (
                                 action,
                                 occurred_at DESC
        );

COMMENT ON TABLE aegis_security_audit_log IS
    'Historical security audit events. No passwords or hashes.';

COMMENT ON COLUMN aegis_security_audit_log.details IS
    'Non-sensitive operational metadata only.';
