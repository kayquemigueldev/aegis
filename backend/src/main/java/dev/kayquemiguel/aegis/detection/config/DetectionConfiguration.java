package dev.kayquemiguel.aegis.detection.config;

import dev.kayquemiguel.aegis.detection.DetectionEngineFactory;
import dev.kayquemiguel.aegis.detection.PossiblePortScanDetector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class DetectionConfiguration {

    private static final Duration PORT_SCAN_WINDOW =
            Duration.ofSeconds(60);

    private static final int PORT_SCAN_THRESHOLD = 3;

    @Bean
    public DetectionEngineFactory detectionEngineFactory() {
        return new DetectionEngineFactory(
                List.of(
                        () -> new PossiblePortScanDetector(
                                PORT_SCAN_WINDOW,
                                PORT_SCAN_THRESHOLD
                        )
                )
        );
    }
}
