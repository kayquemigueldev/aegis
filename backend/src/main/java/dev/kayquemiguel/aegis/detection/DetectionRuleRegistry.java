package dev.kayquemiguel.aegis.detection;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DetectionRuleRegistry {

    private final Map<String, DetectionRule> rules;

    public DetectionRuleRegistry(
            Collection<? extends DetectionRule> rules
    ) {
        Objects.requireNonNull(rules, "rules is required");

        Map<String, DetectionRule> registered =
                new LinkedHashMap<>();

        for (DetectionRule rule : rules) {
            Objects.requireNonNull(rule, "rule is required");

            String id = rule.ruleId();

            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(
                        "Rule ID must not be blank"
                );
            }

            if (!id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) {
                throw new IllegalArgumentException(
                        "Invalid rule ID: " + id
                );
            }

            if (registered.putIfAbsent(id, rule) != null) {
                throw new IllegalArgumentException(
                        "Duplicate detection rule: " + id
                );
            }
        }

        this.rules = Map.copyOf(registered);
    }

    public Optional<DetectionRule> findById(String ruleId) {
        Objects.requireNonNull(ruleId, "ruleId is required");
        return Optional.ofNullable(rules.get(ruleId));
    }

    public List<DetectionRule> findAll() {
        return rules.values().stream()
                .sorted((a, b) ->
                        a.ruleId().compareTo(b.ruleId()))
                .toList();
    }

    public int size() {
        return rules.size();
    }
}
