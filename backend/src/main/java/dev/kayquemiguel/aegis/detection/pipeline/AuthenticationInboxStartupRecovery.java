package dev.kayquemiguel.aegis.detection.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationInboxStartupRecovery {

    private static final Logger log =
            LoggerFactory.getLogger(
                    AuthenticationInboxStartupRecovery.class
            );

    private final DurableAuthenticationProcessor processor;

    public AuthenticationInboxStartupRecovery(
            DurableAuthenticationProcessor processor
    ) {
        this.processor = processor;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverAtStartup() {
        int recovered = processor.recoverPending();

        log.info(
                "AEGIS-101 startup recovery completed: "
                + "{} pending authentication events processed",
                recovered
        );
    }
}
