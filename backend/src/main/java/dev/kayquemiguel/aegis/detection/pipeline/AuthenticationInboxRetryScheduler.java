package dev.kayquemiguel.aegis.detection.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AuthenticationInboxRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(
            AuthenticationInboxRetryScheduler.class
    );

    private final DurableAuthenticationProcessor processor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public AuthenticationInboxRetryScheduler(
            DurableAuthenticationProcessor processor
    ) {
        this.processor = Objects.requireNonNull(processor);
    }

    @Scheduled(
            fixedDelayString =
                    "${aegis.auth.recovery.retry-delay-ms:300000}",
            initialDelayString =
                    "${aegis.auth.recovery.retry-initial-delay-ms:300000}"
    )
    public void retryPending() {
        if (!running.compareAndSet(false, true)) {
            log.debug(
                    "AEGIS-101 recovery skipped: already running"
            );
            return;
        }

        try {
            int recovered = processor.recoverPending();

            if (recovered > 0) {
                log.info(
                        "AEGIS-101 periodic recovery completed: "
                        + "{} pending events processed",
                        recovered
                );
            } else {
                log.debug(
                        "AEGIS-101 periodic recovery: no pending events"
                );
            }
        } catch (org.springframework.dao.DataAccessException exception) {
            log.warn(
                    "AEGIS-101 recovery postponed: "
                    + "database temporarily unavailable",
                    exception
            );
        } catch (RuntimeException exception) {
            log.error(
                    "AEGIS-101 periodic recovery failed; "
                    + "next scheduled attempt remains enabled",
                    exception
            );
        } finally {
            running.set(false);
        }
    }
}
