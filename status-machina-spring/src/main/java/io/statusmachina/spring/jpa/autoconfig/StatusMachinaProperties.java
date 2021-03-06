/*
 *
 *  * Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *
 */

package io.statusmachina.spring.jpa.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.transaction.TransactionDefinition;

@ConfigurationProperties(prefix = "statusmachina.spring")
public class StatusMachinaProperties {
    private int transactionIsolation = TransactionDefinition.ISOLATION_DEFAULT;
    private int transactionPropagation = TransactionDefinition.PROPAGATION_REQUIRES_NEW;

    private TransactionRetry transactionRetry = new TransactionRetry();

    public int getTransactionIsolation() {
        return transactionIsolation;
    }

    public void setTransactionIsolation(int transactionIsolation) {
        this.transactionIsolation = transactionIsolation;
    }

    public int getTransactionPropagation() {
        return transactionPropagation;
    }

    public void setTransactionPropagation(int transactionPropagation) {
        this.transactionPropagation = transactionPropagation;
    }

    public TransactionRetry getTransactionRetry() {
        return transactionRetry;
    }

    public void setTransactionRetry(TransactionRetry transactionRetry) {
        this.transactionRetry = transactionRetry;
    }
}
