
package dev.kayquemiguel.aegis.ingestion.suricata;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;
import dev.kayquemiguel.aegis.domain.event.NetworkProtocol;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigInteger;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class SuricataFlowParser {

    private static final BigInteger MAX_UINT64 =
            new BigInteger("18446744073709551615");

    private final JsonMapper mapper = JsonMapper.builder().build();

    public Optional<NetworkEvent> parse(String raw, Instant ingestedAt) {
        Objects.requireNonNull(raw, "raw is required");
        Objects.requireNonNull(ingestedAt, "ingestedAt is required");

        try {
            JsonNode root = mapper.readTree(raw);

            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException(
                        "Expected a JSON object"
                );
            }

            String eventType = requiredText(root, "event_type");

            if (!"flow".equals(eventType)) {
                return Optional.empty();
            }

            String timestamp = requiredText(root, "timestamp");
            String normalizedTimestamp = timestamp.replaceFirst(
                    "([+-][0-9]{2})([0-9]{2})$", "$1:$2"
            );

            Instant occurredAt = OffsetDateTime
                    .parse(normalizedTimestamp)
                    .toInstant();

            String protocolValue = requiredText(root, "proto")
                    .toUpperCase(Locale.ROOT);

            NetworkProtocol protocol;

            try {
                protocol = NetworkProtocol.valueOf(protocolValue);
            } catch (IllegalArgumentException exception) {
                protocol = NetworkProtocol.OTHER;
            }

            NetworkEvent event = new NetworkEvent(
                    UUID.randomUUID(),
                    occurredAt,
                    ingestedAt,
                    requiredText(root, "src_ip"),
                    requiredText(root, "dest_ip"),
                    optionalPort(root, "src_port"),
                    optionalPort(root, "dest_port"),
                    protocol,
                    "suricata",
                    optionalFlowId(root)
            );

            return Optional.of(event);

        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Invalid Suricata EVE JSON flow event",
                    exception
            );
        }
    }


    private static String requiredText(
            JsonNode root,
            String field
    ) {
        JsonNode value = root.get(field);

        if (value == null || !value.isTextual()
                || value.asString().isBlank()) {
            throw new IllegalArgumentException(
                    "Missing or invalid field: " + field
            );
        }

        return value.asString();
    }


    private static Integer optionalPort(
            JsonNode root,
            String field
    ) {
        JsonNode value = root.get(field);

        if (value == null || value.isNull()) {
            return null;
        }

        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new IllegalArgumentException(
                    "Invalid port field: " + field
            );
        }

        return value.intValue();
    }

    private static String optionalFlowId(JsonNode root) {
        JsonNode value = root.get("flow_id");

        if (value == null || value.isNull()) {
            return null;
        }

        if (!value.isIntegralNumber()) {
            throw new IllegalArgumentException(
                    "flow_id must be an unsigned integer"
            );
        }

        String decimal = value.toString();

        if (!decimal.matches("[0-9]+")) {
            throw new IllegalArgumentException(
                    "flow_id must be an unsigned integer"
            );
        }

        BigInteger number = new BigInteger(decimal);

        if (number.compareTo(MAX_UINT64) > 0) {
            throw new IllegalArgumentException(
                    "flow_id exceeds unsigned 64-bit range"
            );
        }

        return decimal;
    }
}
