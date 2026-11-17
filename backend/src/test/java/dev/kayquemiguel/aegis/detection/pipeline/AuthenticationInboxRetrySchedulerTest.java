package dev.kayquemiguel.aegis.detection.pipeline;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationInboxRetrySchedulerTest {

    @Test
    void shouldRecoverPendingEvents() {
        DurableAuthenticationProcessor processor =
                mock(DurableAuthenticationProcessor.class);

        when(processor.recoverPending()).thenReturn(5);

        AuthenticationInboxRetryScheduler scheduler =
                new AuthenticationInboxRetryScheduler(processor);

        scheduler.retryPending();

        verify(processor, times(1)).recoverPending();
    }

    @Test
    void shouldAllowRetryAfterDatabaseFailure() {
        DurableAuthenticationProcessor processor =
                mock(DurableAuthenticationProcessor.class);

        when(processor.recoverPending())
                .thenThrow(
                        new org.springframework.dao
                                .DataAccessResourceFailureException(
                                        "Simulated database outage"
                                )
                )
                .thenReturn(3);

        AuthenticationInboxRetryScheduler scheduler =
                new AuthenticationInboxRetryScheduler(processor);

        assertDoesNotThrow(scheduler::retryPending);
        assertDoesNotThrow(scheduler::retryPending);

        verify(processor, times(2)).recoverPending();
    }

    @Test
    void shouldSkipOverlappingExecution() throws Exception {
        DurableAuthenticationProcessor processor =
                mock(DurableAuthenticationProcessor.class);

        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        when(processor.recoverPending()).thenAnswer(invocation -> {
            entered.countDown();

            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Recovery test timed out"
                );
            }

            return 1;
        });

        AuthenticationInboxRetryScheduler scheduler =
                new AuthenticationInboxRetryScheduler(processor);

        var executor = Executors.newSingleThreadExecutor();

        try {
            var first = executor.submit(scheduler::retryPending);

            assertTrue(
                    entered.await(10, TimeUnit.SECONDS)
            );

            // The first call is still running.
            // A second call must not reach the processor.
            scheduler.retryPending();

            verify(processor, times(1)).recoverPending();

            release.countDown();

            first.get(10, TimeUnit.SECONDS);

            scheduler.retryPending();

            verify(processor, times(2)).recoverPending();
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }
}
