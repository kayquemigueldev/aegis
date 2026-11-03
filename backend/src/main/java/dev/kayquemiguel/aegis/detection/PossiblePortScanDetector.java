
package dev.kayquemiguel.aegis.detection;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;
import dev.kayquemiguel.aegis.domain.event.NetworkProtocol;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class PossiblePortScanDetector {

    public static final String RULE_ID = "NET-001";

    private final Duration window;
    private final int threshold;

    private final Map<PairKey, PairState> states =
            new HashMap<>();

    public PossiblePortScanDetector(
            Duration window,
            int threshold
    ) {
        Objects.requireNonNull(window, "window is required");

        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException(
                    "Window must be positive"
            );
        }

        if (threshold < 2) {
            throw new IllegalArgumentException(
                    "Threshold must be at least 2"
            );
        }

        this.window = window;
        this.threshold = threshold;
    }

    public synchronized Optional<DetectionAlert> evaluate(
            NetworkEvent event
    ) {
        Objects.requireNonNull(event, "event is required");

        if (event.protocol() != NetworkProtocol.TCP
                || event.destinationPort() == null
                || event.sourceFlowId() == null) {
            return Optional.empty();
        }

        PairKey key = new PairKey(
                event.sourceIp(),
                event.destinationIp()
        );

        PairState state = states.computeIfAbsent(
                key,
                ignored -> new PairState()
        );

        Instant occurredAt = event.occurredAt();

        // MVP: entradas fora da ordem temporal por par
        // origem/destino sao ignoradas.
        if (state.latestTimestamp != null
                && occurredAt.isBefore(state.latestTimestamp)) {
            return Optional.empty();
        }

        state.latestTimestamp = occurredAt;

        // Janela inclusiva: eventos exatamente no
        // limite inferior ainda pertencem a janela.
        Instant cutoff = occurredAt.minus(window);

        while (!state.observations.isEmpty()
                && state.observations.peekFirst()
                .timestamp().isBefore(cutoff)) {
            state.observations.removeFirst();
        }

        // Atualizacoes do mesmo identificador de fluxo
        // nao devem gerar novas observacoes.
        boolean alreadyObserved = state.observations.stream()
                .anyMatch(observation ->
                        observation.flowId()
                                .equals(event.sourceFlowId())
                );

        if (alreadyObserved) {
            return Optional.empty();
        }

        int previousPortCount =
                distinctPortCount(state.observations);

        state.observations.addLast(
                new Observation(
                        occurredAt,
                        event.sourceFlowId(),
                        event.destinationPort()
                )
        );

        int currentPortCount =
                distinctPortCount(state.observations);

        // Emitimos um alerta quando o limiar e
        // atravessado, nao para cada fluxo posterior.
        if (previousPortCount < threshold
                && currentPortCount >= threshold) {

            DetectionAlert alert = new DetectionAlert(
                    RULE_ID,
                    event.sourceIp(),
                    event.destinationIp(),
                    occurredAt,
                    currentPortCount,
                    threshold,
                    "Multiple distinct TCP destination ports "
                            + "observed within the detection window"
            );

            return Optional.of(alert);
        }

        return Optional.empty();
    }

    public synchronized List<DetectionAlert> evaluateAll(
            Iterable<NetworkEvent> events
    ) {
        Objects.requireNonNull(events);

        List<DetectionAlert> alerts = new ArrayList<>();

        for (NetworkEvent event : events) {
            evaluate(event).ifPresent(alerts::add);
        }

        return List.copyOf(alerts);
    }

    private static int distinctPortCount(
            Deque<Observation> observations
    ) {
        Set<Integer> ports = new HashSet<>();

        for (Observation observation : observations) {
            ports.add(observation.destinationPort());
        }

        return ports.size();
    }

    private record PairKey(
            String sourceIp,
            String destinationIp
    ) {
    }

    private record Observation(
            Instant timestamp,
            String flowId,
            int destinationPort
    ) {
    }

    private static final class PairState {
        private Instant latestTimestamp;

        private final Deque<Observation> observations =
                new ArrayDeque<>();
    }
}
