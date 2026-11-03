
package dev.kayquemiguel.aegis.detection;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;
import dev.kayquemiguel.aegis.domain.event.NetworkProtocol;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class PossiblePortScanDetector {

    public static final String RULE_ID = "NET-001";

    private static final Duration DEFAULT_IDLE_TIMEOUT =
            Duration.ofMinutes(10);

    private static final int DEFAULT_MAX_PAIRS = 10_000;
    private static final int DEFAULT_MAX_OBSERVATIONS = 512;

    private final Duration window;
    private final int threshold;
    private final Duration idleTimeout;
    private final int maxPairs;
    private final int maxObservationsPerPair;

    private final Map<PairKey, PairState> states =
            new LinkedHashMap<>(16, 0.75f, true);

    private Instant ingestionWatermark;

    public PossiblePortScanDetector(
            Duration window,
            int threshold
    ) {
        this(
                window,
                threshold,
                DEFAULT_IDLE_TIMEOUT,
                DEFAULT_MAX_PAIRS,
                DEFAULT_MAX_OBSERVATIONS
        );
    }

    public PossiblePortScanDetector(
            Duration window,
            int threshold,
            Duration idleTimeout,
            int maxPairs,
            int maxObservationsPerPair
    ) {
        Objects.requireNonNull(window, "window is required");
        Objects.requireNonNull(
                idleTimeout, "idleTimeout is required"
        );

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

        if (idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException(
                    "Idle timeout must be positive"
            );
        }

        if (maxPairs < 1) {
            throw new IllegalArgumentException(
                    "maxPairs must be positive"
            );
        }

        if (maxObservationsPerPair < threshold) {
            throw new IllegalArgumentException(
                    "Observation limit must be >= threshold"
            );
        }

        this.window = window;
        this.threshold = threshold;
        this.idleTimeout = idleTimeout;
        this.maxPairs = maxPairs;
        this.maxObservationsPerPair = maxObservationsPerPair;
    }

    public synchronized Optional<DetectionAlert> evaluate(
            NetworkEvent event
    ) {
        Objects.requireNonNull(event, "event is required");

        Instant ingestedAt = event.ingestedAt();

        if (ingestionWatermark == null
                || ingestedAt.isAfter(ingestionWatermark)) {
            ingestionWatermark = ingestedAt;
        }

        expireInactivePairs();

        Instant minimumIngestionTime =
                ingestionWatermark.minus(idleTimeout);

        if (ingestedAt.isBefore(minimumIngestionTime)) {
            return Optional.empty();
        }

        if (event.protocol() != NetworkProtocol.TCP
                || event.destinationPort() == null
                || event.sourceFlowId() == null) {
            return Optional.empty();
        }

        // Isolamento por sensor + origem + destino.
        PairKey key = new PairKey(
                event.sensorId(),
                event.sourceIp(),
                event.destinationIp()
        );

        PairState state = states.get(key);

        if (state == null) {
            ensurePairCapacity();
            state = new PairState();
            states.put(key, state);
        }

        Instant occurredAt = event.occurredAt();

        // MVP: eventos fora de ordem por grupo
        // ainda nao sao processados.
        if (state.latestTimestamp != null
                && occurredAt.isBefore(state.latestTimestamp)) {
            return Optional.empty();
        }

        state.latestTimestamp = occurredAt;

        if (state.lastSeenIngestedAt == null
                || ingestedAt.isAfter(state.lastSeenIngestedAt)) {
            state.lastSeenIngestedAt = ingestedAt;
        }

        Instant cutoff = occurredAt.minus(window);

        while (!state.observations.isEmpty()
                && state.observations.peekFirst()
                .timestamp().isBefore(cutoff)) {
            state.observations.removeFirst();
        }

        boolean alreadyObserved =
                state.observations.stream()
                        .anyMatch(observation ->
                                observation.flowId().equals(
                                        event.sourceFlowId()
                                )
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

        while (state.observations.size()
                > maxObservationsPerPair) {
            state.observations.removeFirst();
        }

        int currentPortCount =
                distinctPortCount(state.observations);

        if (previousPortCount < threshold
                && currentPortCount >= threshold) {

            return Optional.of(new DetectionAlert(
                    RULE_ID,
                    event.sourceIp(),
                    event.destinationIp(),
                    occurredAt,
                    currentPortCount,
                    threshold,
                    "Multiple distinct TCP destination ports "
                            + "observed within the detection window",
                    event.sensorId()
            ));
        }

        return Optional.empty();
    }

    public synchronized List<DetectionAlert> evaluateAll(
            Iterable<NetworkEvent> events
    ) {
        Objects.requireNonNull(events, "events is required");

        List<DetectionAlert> alerts = new ArrayList<>();

        for (NetworkEvent event : events) {
            evaluate(event).ifPresent(alerts::add);
        }

        return List.copyOf(alerts);
    }

    private void expireInactivePairs() {
        if (ingestionWatermark == null) {
            return;
        }

        Instant cutoff =
                ingestionWatermark.minus(idleTimeout);

        Iterator<Map.Entry<PairKey, PairState>> iterator =
                states.entrySet().iterator();

        while (iterator.hasNext()) {
            PairState state = iterator.next().getValue();

            if (state.lastSeenIngestedAt != null
                    && state.lastSeenIngestedAt
                    .isBefore(cutoff)) {
                iterator.remove();
            }
        }
    }

    private void ensurePairCapacity() {
        while (states.size() >= maxPairs) {
            Iterator<PairKey> iterator =
                    states.keySet().iterator();

            if (!iterator.hasNext()) {
                return;
            }

            iterator.next();
            iterator.remove();
        }
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

    synchronized int trackedPairCount() {
        return states.size();
    }

    synchronized int trackedObservationCount(
            String sourceIp,
            String destinationIp
    ) {
        return trackedObservationCount(
                NetworkEvent.LEGACY_SENSOR_ID,
                sourceIp,
                destinationIp
        );
    }

    synchronized int trackedObservationCount(
            String sensorId,
            String sourceIp,
            String destinationIp
    ) {
        // Evita modificar a ordem LRU durante a consulta.
        for (Map.Entry<PairKey, PairState> entry :
                states.entrySet()) {
            PairKey key = entry.getKey();

            if (key.sensorId().equals(sensorId)
                    && key.sourceIp().equals(sourceIp)
                    && key.destinationIp().equals(destinationIp)) {
                return entry.getValue().observations.size();
            }
        }

        return 0;
    }

    private record PairKey(
            String sensorId,
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
        private Instant lastSeenIngestedAt;

        private final Deque<Observation> observations =
                new ArrayDeque<>();
    }
}
