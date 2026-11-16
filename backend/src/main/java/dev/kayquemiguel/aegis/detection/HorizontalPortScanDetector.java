package dev.kayquemiguel.aegis.detection;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;
import dev.kayquemiguel.aegis.domain.event.NetworkProtocol;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class HorizontalPortScanDetector
        implements DetectionRule {

    public static final String RULE_ID = "NET-002";

    private static final Duration IDLE_TIMEOUT =
            Duration.ofMinutes(10);

    private static final int MAX_GROUPS = 10_000;
    private static final int MAX_OBSERVATIONS = 512;

    private final Duration window;
    private final int threshold;

    private final Map<GroupKey, GroupState> states =
            new LinkedHashMap<>(16, 0.75f, true);

    private Instant ingestionWatermark;

    public HorizontalPortScanDetector(
            Duration window,
            int threshold
    ) {
        Objects.requireNonNull(
                window, "window is required"
        );

        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException(
                    "Window must be positive"
            );
        }

        if (threshold < 2 || threshold > MAX_OBSERVATIONS) {
            throw new IllegalArgumentException(
                    "Invalid detection threshold"
            );
        }

        this.window = window;
        this.threshold = threshold;
    }

    @Override
    public String ruleId() {
        return RULE_ID;
    }

    @Override
    public synchronized Optional<DetectionAlert> evaluate(
            NetworkEvent event
    ) {
        Objects.requireNonNull(event, "event is required");

        Instant ingestedAt = event.ingestedAt();

        if (ingestionWatermark == null
                || ingestedAt.isAfter(ingestionWatermark)) {
            ingestionWatermark = ingestedAt;
        }

        expireInactiveGroups();

        if (ingestedAt.isBefore(
                ingestionWatermark.minus(IDLE_TIMEOUT))) {
            return Optional.empty();
        }

        if (event.protocol() != NetworkProtocol.TCP
                || event.destinationPort() == null
                || event.sourceFlowId() == null) {
            return Optional.empty();
        }

        GroupKey key = new GroupKey(
                event.sensorId(),
                event.sourceIp(),
                event.destinationPort()
        );

        GroupState state = states.get(key);

        if (state == null) {
            ensureCapacity();
            state = new GroupState();
            states.put(key, state);
        }

        Instant occurredAt = event.occurredAt();

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

        boolean duplicateFlow =
                state.observations.stream()
                        .anyMatch(observation ->
                                observation.flowId().equals(
                                        event.sourceFlowId()
                                )
                        );

        if (duplicateFlow) {
            return Optional.empty();
        }

        int previousCount =
                distinctDestinationCount(state.observations);

        state.observations.addLast(
                new Observation(
                        occurredAt,
                        event.sourceFlowId(),
                        event.destinationIp()
                )
        );

        while (state.observations.size()
                > MAX_OBSERVATIONS) {
            state.observations.removeFirst();
        }

        int currentCount =
                distinctDestinationCount(state.observations);

        if (previousCount < threshold
                && currentCount >= threshold) {

            return Optional.of(new DetectionAlert(
                    RULE_ID,
                    event.sourceIp(),
                    event.destinationIp(),
                    occurredAt,
                    currentCount,
                    threshold,
                    "Horizontal TCP port scan: multiple "
                            + "destination hosts contacted "
                            + "on the same port",
                    event.sensorId()
            ));
        }

        return Optional.empty();
    }

    private void expireInactiveGroups() {
        if (ingestionWatermark == null) {
            return;
        }

        Instant cutoff =
                ingestionWatermark.minus(IDLE_TIMEOUT);

        Iterator<Map.Entry<GroupKey, GroupState>> iterator =
                states.entrySet().iterator();

        while (iterator.hasNext()) {
            GroupState state =
                    iterator.next().getValue();

            if (state.lastSeenIngestedAt != null
                    && state.lastSeenIngestedAt.isBefore(cutoff)) {
                iterator.remove();
            }
        }
    }

    private void ensureCapacity() {
        while (states.size() >= MAX_GROUPS) {
            Iterator<GroupKey> iterator =
                    states.keySet().iterator();

            if (!iterator.hasNext()) {
                return;
            }

            iterator.next();
            iterator.remove();
        }
    }

    private static int distinctDestinationCount(
            Deque<Observation> observations
    ) {
        Set<String> destinations = new HashSet<>();

        for (Observation observation : observations) {
            destinations.add(observation.destinationIp());
        }

        return destinations.size();
    }

    private record GroupKey(
            String sensorId,
            String sourceIp,
            int destinationPort
    ) {
    }

    private record Observation(
            Instant timestamp,
            String flowId,
            String destinationIp
    ) {
    }

    private static final class GroupState {
        private Instant latestTimestamp;
        private Instant lastSeenIngestedAt;

        private final Deque<Observation> observations =
                new ArrayDeque<>();
    }
}
