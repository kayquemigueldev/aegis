package dev.kayquemiguel.aegis.detection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class DetectionEngineFactory {

    private final List<Supplier<? extends DetectionRule>> ruleFactories;

    public DetectionEngineFactory(
            Collection<? extends Supplier<? extends DetectionRule>> ruleFactories
    ) {
        Objects.requireNonNull(
                ruleFactories, "ruleFactories is required"
        );

        List<Supplier<? extends DetectionRule>> validated =
                new ArrayList<>();

        for (Supplier<? extends DetectionRule> factory : ruleFactories) {
            validated.add(Objects.requireNonNull(
                    factory, "rule factory is required"
            ));
        }

        this.ruleFactories = List.copyOf(validated);
    }

    public DetectionEngine create() {
        List<DetectionRule> rules = new ArrayList<>();

        for (Supplier<? extends DetectionRule> factory : ruleFactories) {
            rules.add(Objects.requireNonNull(
                    factory.get(),
                    "rule factory returned null"
            ));
        }

        return new DetectionEngine(
                new DetectionRuleRegistry(rules)
        );
    }

    public int configuredRuleCount() {
        return ruleFactories.size();
    }
}
