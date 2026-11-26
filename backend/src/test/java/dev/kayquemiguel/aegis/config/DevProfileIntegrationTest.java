package dev.kayquemiguel.aegis.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class DevProfileIntegrationTest {

    @Autowired
    private Environment environment;

    @Test
    void developmentProfileStartsSuccessfully() {
        assertTrue(
            environment.acceptsProfiles("dev")
        );

        assertEquals(
            "development",
            environment.getProperty(
                "aegis.environment.name"
            )
        );

        assertEquals(
            "validate",
            environment.getProperty(
                "spring.jpa.hibernate.ddl-auto"
            )
        );
    }
}
