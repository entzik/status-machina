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

import com.google.common.collect.ImmutableMap;
import io.statusmachina.spring.jpa.autoconfig.StatusMachinaProperties;
import org.hibernate.TransactionException;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryTemplateConfiguration {
    public static final String RETRY_TEMPLATE_TRANSACTION_RETRY = "transactionRetryTemplate";

    @Autowired
    StatusMachinaProperties properties;

    @Bean(name = RETRY_TEMPLATE_TRANSACTION_RETRY)
    public RetryTemplate configureStateMachineTransactionRetryTemplate() {

        RetryTemplate template = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(properties.getTransactionRetry().getInitialInterval());
        backOffPolicy.setMaxInterval(properties.getTransactionRetry().getMaxInterval());
        backOffPolicy.setMultiplier(properties.getTransactionRetry().getMultiplier());
        template.setBackOffPolicy(backOffPolicy);

        final ImmutableMap<Class<? extends Throwable>, Boolean> build = ImmutableMap.<Class<? extends Throwable>, Boolean>builder()
                .put(TransactionException.class, true)
                .put(LockAcquisitionException.class, true)
                .put(CannotAcquireLockException.class, true)
                .build();
        RetryPolicy retryPolicy = new SimpleRetryPolicy(properties.getTransactionRetry().getMaxAttempts(), build, true);
        template.setRetryPolicy(retryPolicy);

        return template;
    }
}
