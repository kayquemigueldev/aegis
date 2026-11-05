
package dev.kayquemiguel.aegis.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SensorAccessPolicy {

    public static final String SENSOR_PREFIX = "SENSOR:";

    public boolean isAdmin(Authentication authentication) {
        return hasAuthority(authentication, "ROLE_ADMIN");
    }

    public Set<String> permittedSensors(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            return Set.of();
        }

        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(value ->
                        value.startsWith(SENSOR_PREFIX)
                )
                .map(value ->
                        value.substring(SENSOR_PREFIX.length())
                )
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean canRead(
            Authentication authentication,
            String sensorId
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        if (!hasAuthority(
                authentication,
                "ROLE_ANALYST"
        )) {
            return false;
        }

        return sensorId != null
                && permittedSensors(authentication)
                .contains(sensorId);
    }

    public void requireRead(
            Authentication authentication,
            String sensorId
    ) {
        if (!canRead(authentication, sensorId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Sensor access denied"
            );
        }
    }

    private boolean hasAuthority(
            Authentication authentication,
            String authority
    ) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities()
                .stream()
                .anyMatch(granted ->
                        authority.equals(
                                granted.getAuthority()
                        )
                );
    }
}
