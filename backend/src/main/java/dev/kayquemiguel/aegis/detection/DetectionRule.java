package dev.kayquemiguel.aegis.detection;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;

import java.util.Optional;

public interface DetectionRule {

    String ruleId();

    Optional<DetectionAlert> evaluate(NetworkEvent event);
}
