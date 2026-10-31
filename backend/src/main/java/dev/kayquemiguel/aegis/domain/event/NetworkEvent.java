package dev.kayquemiguel.aegis.domain.event;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record NetworkEvent(
        UUID id,
        Instant occurredAt,
        Instant ingestedAt,
        String sourceIp,
        String destinationIp,
        Integer sourcePort,
        Integer destinationPort,
        NetworkProtocol protocol,
        String sourceSystem
) {

    public NetworkEvent {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        Objects.requireNonNull(ingestedAt, "ingestedAt is required");
        Objects.requireNonNull(protocol, "protocol is required");

        sourceIp = validateIp(sourceIp, "sourceIp");
        destinationIp = validateIp(destinationIp, "destinationIp");

        validatePort(sourcePort, "sourcePort");
        validatePort(destinationPort, "destinationPort");

        if (protocol == NetworkProtocol.ICMP
                || protocol == NetworkProtocol.ICMPV6) {
            if (sourcePort != null || destinationPort != null) {
                throw new IllegalArgumentException(
                        "ICMP events must not contain transport ports"
                );
            }
        }

        if (sourceSystem == null || sourceSystem.isBlank()) {
            throw new IllegalArgumentException(
                    "sourceSystem is required"
            );
        }

        sourceSystem = sourceSystem.trim();
    }

    private static void validatePort(Integer port, String field) {
        if (port != null && (port < 0 || port > 65535)) {
            throw new IllegalArgumentException(
                    field + " must be between 0 and 65535"
            );
        }
    }

    private static String validateIp(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }

        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(
                    field + " must not contain surrounding whitespace"
            );
        }

        if (value.contains(":")) {
            try {
                if (value.contains("%")) {
                    throw new IllegalArgumentException(
                            field + " must not contain a scope identifier"
                    );
                }

                InetAddress address = InetAddress.getByName(value);

                if (!(address instanceof Inet6Address)) {
                    throw new IllegalArgumentException(
                            field + " must be a valid IPv6 address"
                    );
                }

                return address.getHostAddress();
            } catch (UnknownHostException ex) {
                throw new IllegalArgumentException(
                        field + " must be a valid IPv6 address", ex
                );
            }
        }

        String[] octets = value.split("\\.", -1);

        if (octets.length != 4) {
            throw new IllegalArgumentException(
                    field + " must be a valid IP address"
            );
        }

        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3) {
                throw new IllegalArgumentException(
                        field + " must be a valid IPv4 address"
                );
            }

            for (int i = 0; i < octet.length(); i++) {
                char ch = octet.charAt(i);

                if (ch < '0' || ch > '9') {
                    throw new IllegalArgumentException(
                            field + " must be a valid IPv4 address"
                    );
                }
            }

            int number = Integer.parseInt(octet);

            if (number > 255) {
                throw new IllegalArgumentException(
                        field + " must be a valid IPv4 address"
                );
            }

            if (octet.length() > 1 && octet.charAt(0) == '0') {
                throw new IllegalArgumentException(
                        field + " must not contain leading zeroes"
                );
            }
        }

        return value;
    }
}
