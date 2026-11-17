package dev.kayquemiguel.aegis.domain.event;

import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuthenticationEvent(
        UUID id,
        Instant occurredAt,
        Instant ingestedAt,
        String sourceIp,
        String username,
        String sourceSystem,
        String sensorId,
        AuthenticationOutcome outcome
) {
    public AuthenticationEvent {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(
                occurredAt, "occurredAt is required"
        );
        Objects.requireNonNull(
                ingestedAt, "ingestedAt is required"
        );
        Objects.requireNonNull(
                outcome, "outcome is required"
        );

        if (sourceIp == null
                || sourceIp.isBlank()
                || !sourceIp.equals(sourceIp.trim())) {
            throw new IllegalArgumentException(
                    "sourceIp is required and must be trimmed"
            );
        }

        validateSourceIp(sourceIp);

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "username is required"
            );
        }

        username = username.trim();

        if (username.length() > 256) {
            throw new IllegalArgumentException(
                    "username must contain at most 256 characters"
            );
        }

        if (sourceSystem == null || sourceSystem.isBlank()) {
            throw new IllegalArgumentException(
                    "sourceSystem is required"
            );
        }

        sourceSystem = sourceSystem.trim();

        if (sensorId == null
                || !sensorId.trim().matches(
                        "[A-Za-z0-9][A-Za-z0-9._-]{0,63}"
                )) {
            throw new IllegalArgumentException(
                    "Invalid sensorId"
            );
        }

        sensorId = sensorId.trim();
    }

    private static void validateSourceIp(String sourceIp) {
        if (sourceIp.contains(":")) {
            if (!sourceIp.matches("[0-9A-Fa-f:.]+")) {
                throw new IllegalArgumentException(
                        "Invalid IPv6 address"
                );
            }

            try {
                InetAddress parsed =
                        InetAddress.getByName(sourceIp);

                if (!(parsed instanceof Inet6Address)) {
                    throw new IllegalArgumentException(
                            "Invalid IPv6 address"
                    );
                }
            } catch (UnknownHostException exception) {
                throw new IllegalArgumentException(
                        "Invalid IPv6 address",
                        exception
                );
            }

            return;
        }

        String[] octets = sourceIp.split("\\.", -1);

        if (octets.length != 4) {
            throw new IllegalArgumentException(
                    "Invalid IPv4 address"
            );
        }

        for (String octet : octets) {
            if (octet.isEmpty()
                    || octet.length() > 3
                    || (octet.length() > 1
                        && octet.charAt(0) == '0')) {
                throw new IllegalArgumentException(
                        "Invalid IPv4 address"
                );
            }

            for (int i = 0; i < octet.length(); i++) {
                char ch = octet.charAt(i);

                if (ch < '0' || ch > '9') {
                    throw new IllegalArgumentException(
                            "Invalid IPv4 address"
                    );
                }
            }

            if (Integer.parseInt(octet) > 255) {
                throw new IllegalArgumentException(
                        "Invalid IPv4 address"
                );
            }
        }
    }

    public boolean isFailure() {
        return outcome == AuthenticationOutcome.FAILURE;
    }
}
