package dev.kayquemiguel.aegis.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public final class CorrelationIdFilter
        extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-ID";
    public static final String ATTRIBUTE =
            CorrelationIdFilter.class.getName() + ".id";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String correlationId = normalize(
                request.getHeader(HEADER)
        );

        request.setAttribute(ATTRIBUTE, correlationId);
        response.setHeader(HEADER, correlationId);

        String previous = MDC.get(MDC_KEY);

        try {
            MDC.put(MDC_KEY, correlationId);
            chain.doFilter(request, response);
        } finally {
            if (previous == null) {
                MDC.remove(MDC_KEY);
            } else {
                MDC.put(MDC_KEY, previous);
            }
        }
    }

    public static String from(HttpServletRequest request) {
        Object value = request.getAttribute(ATTRIBUTE);

        return value instanceof String id
                ? id
                : null;
    }

    private static String normalize(String candidate) {
        if (candidate != null
                && candidate.length() == 36) {
            try {
                UUID id = UUID.fromString(candidate);

                if (id.toString().equals(candidate)) {
                    return candidate;
                }
            } catch (IllegalArgumentException ignored) {
                // Generate a new server-controlled identifier.
            }
        }

        return UUID.randomUUID().toString();
    }
}
