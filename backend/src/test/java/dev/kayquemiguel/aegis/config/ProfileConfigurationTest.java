package dev.kayquemiguel.aegis.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ProfileConfigurationTest {

    private Properties load(String profile)
            throws IOException {

        String filename =
                "application-" + profile + ".properties";

        try (InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream(filename)) {

            assertNotNull(input, filename + " is missing");

            Properties properties = new Properties();
            properties.load(input);
            return properties;
        }
    }

    @Test
    void developmentProfileHasExpectedDefaults()
            throws IOException {

        Properties properties = load("dev");

        assertEquals(
                "development",
                properties.getProperty(
                        "aegis.environment.name")
        );

        assertEquals(
                "${AEGIS_SECURITY_MODE:local}",
                properties.getProperty(
                        "aegis.security.mode")
        );
    }

    @Test
    void testProfileHasExpectedDefaults()
            throws IOException {

        Properties properties = load("test");

        assertEquals(
                "test",
                properties.getProperty(
                        "aegis.environment.name")
        );

        assertEquals(
                "${AEGIS_SECURITY_MODE:database}",
                properties.getProperty(
                        "aegis.security.mode")
        );
    }

    @Test
    void profilesPreserveFlywayAndSchemaValidation()
            throws IOException {

        for (String profile :
                new String[]{"dev", "test"}) {

            Properties properties = load(profile);

            assertEquals(
                    "true",
                    properties.getProperty(
                            "spring.flyway.enabled")
            );

            assertEquals(
                    "validate",
                    properties.getProperty(
                            "spring.jpa.hibernate.ddl-auto")
            );

            assertNull(
                    properties.getProperty(
                            "spring.datasource.password")
            );
        }
    }
}
