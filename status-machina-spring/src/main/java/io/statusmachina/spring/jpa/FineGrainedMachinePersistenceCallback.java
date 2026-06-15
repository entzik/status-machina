package io.statusmachina.spring.jpa;

import io.statusmachina.core.api.Machine;
import io.statusmachina.core.spi.MachinePersistenceCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.Retryable;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.Callable;

import static io.statusmachina.spring.jpa.configuration.StateMachineRetryTemplateConfiguration.RETRY_TEMPLATE_TRANSACTION_RETRY;
import static io.statusmachina.spring.jpa.configuration.TransactionTemplateCnfiguration.STATUS_MACHINA_TRANSACTION_TEMPLATE;

@Service
class FineGrainedMachinePersistenceCallback<S, E> implements MachinePersistenceCallback<S, E> {
    @Autowired
    @Lazy
    private SpringJpaStateMachineService stateMachineService;

    @Autowired
    @Qualifier(STATUS_MACHINA_TRANSACTION_TEMPLATE)
    private TransactionTemplate transactionTemplate;

    @Autowired
    @Qualifier(RETRY_TEMPLATE_TRANSACTION_RETRY)
    private RetryTemplate transactionRetryTemplate;

    @Override
    public Machine<S, E> saveNew(Machine<S, E> machine) {
        return runWithRetry(() -> {
            stateMachineService.create(machine);
            return machine;
        });
    }

    @Override
    public Machine<S, E> update(Machine<S, E> machine, long epochMilliForUpdate) {
        return runWithRetry(() -> {
            stateMachineService.update(machine, epochMilliForUpdate);
            return machine;
        });
    }

    @Override
    public <R> R runInTransaction(Callable<R> callable) throws Exception {
        return runWithRetry(() -> transactionTemplate.execute(status -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }));
    }

    /**
     * Executes the given operation through the retry template. When the retry policy is exhausted
     * Spring Framework wraps the last failure in a {@link RetryException}; we rethrow that original
     * cause so callers observe the same exception they would have seen without retries, matching the
     * behaviour of the previous spring-retry based implementation.
     */
    private <R> R runWithRetry(Retryable<R> retryable) {
        try {
            return transactionRetryTemplate.execute(retryable);
        } catch (RetryException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException(cause != null ? cause : e);
        }
    }
}
