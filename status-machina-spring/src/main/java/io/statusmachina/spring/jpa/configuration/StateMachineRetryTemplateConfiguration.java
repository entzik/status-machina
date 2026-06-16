/*
 *  Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */

package io.statusmachina.spring.jpa.configuration;

import io.statusmachina.spring.jpa.autoconfig.StatusMachinaProperties;
import org.hibernate.TransactionException;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.dao.CannotAcquireLockException;

import java.time.Duration;

@Configuration
public class StateMachineRetryTemplateConfiguration {
    public static final String RETRY_TEMPLATE_TRANSACTION_RETRY = "stateMachineTransactionRetryTemplate";

    @Autowired
    StatusMachinaProperties properties;

    @Bean(name = RETRY_TEMPLATE_TRANSACTION_RETRY)
    public RetryTemplate configureStateMachineTransactionRetryTemplate() {
        // maxAttempts historically meant the total number of invocations (initial + retries),
        // whereas Spring Framework's native RetryPolicy counts retries that follow the first
        // invocation, so we subtract one to preserve the previous behaviour.
        final long maxRetries = Math.max(0, properties.getTransactionRetry().getMaxAttempts() - 1);

        // includes() matches against the thrown exception and its nested causes, which preserves
        // the traverseCauses=true semantics of the previous SimpleRetryPolicy configuration.
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .maxRetries(maxRetries)
                .delay(Duration.ofMillis(properties.getTransactionRetry().getInitialInterval()))
                .maxDelay(Duration.ofMillis(properties.getTransactionRetry().getMaxInterval()))
                .multiplier(properties.getTransactionRetry().getMultiplier())
                .includes(TransactionException.class, LockAcquisitionException.class, CannotAcquireLockException.class)
                .build();

        return new RetryTemplate(retryPolicy);
    }
}
