package dev.kayquemiguel.aegis.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TestProfileIntegrationTest {

    @Autowired
    private Environment environment;

    @Test
    void testProfileStartsSuccessfully() {
        assertTrue(
            environment.acceptsProfiles(Profiles.of("test"))
        );

        assertEquals(
            "test",
            environment.getProperty(
                "aegis.environment.name"
            )
        );

        assertEquals(
            "database",
            environment.getProperty(
                "aegis.security.mode"
            )
        );

        assertEquals(
            "true",
            environment.getProperty(
                "spring.flyway.enabled"
            )
        );
    }
}
