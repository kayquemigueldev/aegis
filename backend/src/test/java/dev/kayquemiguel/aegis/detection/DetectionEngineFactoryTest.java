package dev.kayquemiguel.aegis.detection;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class DetectionEngineFactoryTest {

    @Test
    void shouldCreateEngineWithMultipleRules() {
        DetectionEngineFactory factory =
                new DetectionEngineFactory(List.of(
                        () -> rule("NET-001"),
                        () -> rule("NET-002")
                ));

        assertEquals(2, factory.configuredRuleCount());
        assertEquals(2, factory.create().ruleCount());
    }

    @Test
    void shouldCreateFreshRuleInstancesForEachEngine() {
        AtomicInteger instances = new AtomicInteger();

        Supplier<DetectionRule> supplier = () -> {
            instances.incrementAndGet();
            return rule("NET-001");
        };

        DetectionEngineFactory factory =
                new DetectionEngineFactory(List.of(supplier));

        DetectionEngine first = factory.create();
        DetectionEngine second = factory.create();

        assertNotSame(first, second);
        assertEquals(1, first.ruleCount());
        assertEquals(1, second.ruleCount());
        assertEquals(2, instances.get());
    }

    @Test
    void shouldRejectDuplicateRuleIds() {
        DetectionEngineFactory factory =
                new DetectionEngineFactory(List.of(
                        () -> rule("NET-001"),
                        () -> rule("NET-001")
                ));

        assertThrows(
                IllegalArgumentException.class,
                factory::create
        );
    }

    @Test
    void shouldRejectNullFactoriesAndRules() {
        assertThrows(
                NullPointerException.class,
                () -> new DetectionEngineFactory(null)
        );

        assertThrows(
                NullPointerException.class,
                () -> new DetectionEngineFactory(
                        java.util.Arrays.asList(
                                (Supplier<DetectionRule>) null
                        )
                )
        );

        DetectionEngineFactory factory =
                new DetectionEngineFactory(List.of(
                        (Supplier<DetectionRule>) () -> null
                ));

        assertThrows(
                NullPointerException.class,
                factory::create
        );
    }

    @Test
    void shouldSupportEmptyConfiguration() {
        DetectionEngineFactory factory =
                new DetectionEngineFactory(List.of());

        assertEquals(0, factory.configuredRuleCount());
        assertEquals(0, factory.create().ruleCount());
    }

    private static DetectionRule rule(String id) {
        return new DetectionRule() {

            @Override
            public String ruleId() {
                return id;
            }

            @Override
            public Optional<DetectionAlert> evaluate(
                    NetworkEvent event
            ) {
                return Optional.empty();
            }
        };
    }
}
