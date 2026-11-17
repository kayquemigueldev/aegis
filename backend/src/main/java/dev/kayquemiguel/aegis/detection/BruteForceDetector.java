package dev.kayquemiguel.aegis.detection;

import dev.kayquemiguel.aegis.domain.event.AuthenticationEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class BruteForceDetector {

    public static final String RULE_ID = "NET-003";

    private static final Duration IDLE_TIMEOUT =
            Duration.ofMinutes(10);

    private static final Duration MAX_EVENT_LATENESS =
            Duration.ofSeconds(10);

    private static final int MAX_GROUPS = 10_000;
    private static final int MAX_OBSERVATIONS = 512;

    private final Duration window;
    private final int threshold;

    private final Map<GroupKey, GroupState> states =
            new LinkedHashMap<>(16, 0.75f, true);

    private Instant ingestionWatermark;

    public BruteForceDetector(
            Duration window,
            int threshold
    ) {
        Objects.requireNonNull(window, "window is required");

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

    public synchronized Optional<AuthenticationDetectionAlert>
    evaluate(AuthenticationEvent event) {
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

        if (!event.isFailure()) {
            return Optional.empty();
        }

        GroupKey key = new GroupKey(
                event.sensorId(),
                event.sourceSystem(),
                event.sourceIp(),
                event.username()
        );

        GroupState state = states.get(key);

        if (state == null) {
            ensureCapacity();
            state = new GroupState();
            states.put(key, state);
        }

        Instant occurredAt = event.occurredAt();

        if (state.latestTimestamp != null
                && occurredAt.isBefore(
                        state.latestTimestamp.minus(
                                MAX_EVENT_LATENESS
                        ))) {
            return Optional.empty();
        }

        if (state.latestTimestamp == null
                || occurredAt.isAfter(state.latestTimestamp)) {
            state.latestTimestamp = occurredAt;
        }

        if (state.lastSeenIngestedAt == null
                || ingestedAt.isAfter(state.lastSeenIngestedAt)) {
            state.lastSeenIngestedAt = ingestedAt;
        }

        Instant cutoff =
                state.latestTimestamp.minus(window);

        while (!state.observations.isEmpty()
                && state.observations.peekFirst()
                .timestamp().isBefore(cutoff)) {
            state.observations.removeFirst();
        }

        // Um evento atrasado fora da janela nao conta.
        if (occurredAt.isBefore(cutoff)) {
            return Optional.empty();
        }

        boolean duplicated = state.observations.stream()
                .anyMatch(observation ->
                        observation.eventId().equals(event.id()));

        if (duplicated) {
            return Optional.empty();
        }

        int previousCount = state.observations.size();

        Observation incoming =
                new Observation(occurredAt, event.id());

        if (state.observations.isEmpty()
                || !occurredAt.isBefore(
                        state.observations.peekLast().timestamp())) {
            state.observations.addLast(incoming);
        } else {
            List<Observation> ordered =
                    new ArrayList<>(state.observations);

            ordered.add(incoming);
            ordered.sort(
                    Comparator.comparing(Observation::timestamp)
            );

            state.observations.clear();
            state.observations.addAll(ordered);
        }

        while (state.observations.size()
                > MAX_OBSERVATIONS) {
            state.observations.removeFirst();
        }

        int currentCount = state.observations.size();

        if (previousCount < threshold
                && currentCount >= threshold) {
            return Optional.of(
                    new AuthenticationDetectionAlert(
                            RULE_ID,
                            state.latestTimestamp,
                            event.sensorId(),
                            event.sourceSystem(),
                            event.sourceIp(),
                            event.username(),
                            currentCount,
                            threshold
                    )
            );
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
            GroupState state = iterator.next().getValue();

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

            iterator.next();
            iterator.remove();
        }
    }

    synchronized int trackedGroupCount() {
        return states.size();
    }

    synchronized int trackedObservationCount(
            String sensorId,
            String sourceSystem,
            String sourceIp,
            String username
    ) {
        GroupKey key = new GroupKey(
                sensorId,
                sourceSystem,
                sourceIp,
                username
        );

        GroupState state = states.get(key);

        return state == null
                ? 0
                : state.observations.size();
    }

    private record GroupKey(
            String sensorId,
            String sourceSystem,
            String sourceIp,
            String username
    ) {
    }

    private record Observation(
            Instant timestamp,
            UUID eventId
    ) {
    }

    private static final class GroupState {
        private Instant latestTimestamp;
        private Instant lastSeenIngestedAt;

        private final Deque<Observation> observations =
                new ArrayDeque<>();
    }
}
