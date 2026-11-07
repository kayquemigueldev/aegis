
CREATE TABLE aegis_users (
                             id UUID PRIMARY KEY,
                             username VARCHAR(64) NOT NULL UNIQUE,
                             password_hash VARCHAR(100) NOT NULL,
                             role VARCHAR(16) NOT NULL,
                             enabled BOOLEAN NOT NULL DEFAULT TRUE,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT chk_aegis_users_role
                                 CHECK (role IN ('ADMIN', 'ANALYST')),

                             CONSTRAINT chk_aegis_users_username
                                 CHECK (
                                     username ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
),

    CONSTRAINT chk_aegis_users_password_hash
        CHECK (
            password_hash ~
            '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$'
        )
);

CREATE TABLE aegis_user_sensors (
                                    user_id UUID NOT NULL,
                                    sensor_id VARCHAR(64) NOT NULL,

                                    CONSTRAINT pk_aegis_user_sensors
                                        PRIMARY KEY (user_id, sensor_id),

                                    CONSTRAINT fk_aegis_user_sensors_user
                                        FOREIGN KEY (user_id)
                                            REFERENCES aegis_users(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT chk_aegis_user_sensors_sensor
                                        CHECK (
                                            sensor_id ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
)
    );

CREATE INDEX idx_aegis_user_sensors_sensor
    ON aegis_user_sensors (sensor_id);
