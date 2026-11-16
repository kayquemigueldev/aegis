package dev.kayquemiguel.aegis.detection;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DetectionEngine {

    private final List<DetectionRule> rules;

    public DetectionEngine(DetectionRuleRegistry registry) {
        Objects.requireNonNull(
                registry, "registry is required"
        );

        this.rules = registry.findAll();
    }

    public List<DetectionAlert> evaluate(NetworkEvent event) {
        Objects.requireNonNull(
                event, "event is required"
        );

        List<DetectionAlert> alerts = new ArrayList<>();

        for (DetectionRule rule : rules) {
            rule.evaluate(event).ifPresent(alerts::add);
        }

        return List.copyOf(alerts);
    }

    public int ruleCount() {
        return rules.size();
    }
}
