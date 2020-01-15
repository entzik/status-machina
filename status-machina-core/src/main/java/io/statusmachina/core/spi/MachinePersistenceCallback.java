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

package io.statusmachina.core.spi;

import io.statusmachina.core.api.Machine;

import java.util.concurrent.Callable;

/**
 * An interface that provides persistence and transactional services to the state machine.
 * <p>
 * To be implemented by service providers
 * <p>
 * There is no assumption whatsoever of what this storage may be. For what it matters, it can be a relational or document database,
 * a key/value store or a file.
 * <p>
 * State machine builders should configure state machines to use instances of particular implementations of this
 * interface.
 *
 * @param <S> the type of the machine's state
 * @param <E> the type of events the machine receives
 */
public interface MachinePersistenceCallback<S, E> {
    /**
     * Saves a new state machine instance to the underlying storage.
     *
     * @param machine the machine instance who's state is  to be saved to external storage
     *
     * @return an instance of same the state machine, configured with an ID if one was generated during persistence
     */
    Machine<S, E> saveNew(Machine<S, E> machine);

    /**
     * Updates an existing state machine
     *
     * @param machine the machine instance who's state stored in external storage needs to be updated
     *
     * @return an updated instance of the machine
     */
    Machine<S, E> update(Machine<S, E> machine);

    /**
     * Executes a {@link Callable} in a transactional context
     *
     * This method must start a transaction, execute the {@link Callable}, commit the transaction.
     *
     * The state machine will typically use it in two cases:
     *
     * 1: to save the initial internal state when the machine is created
     * 2: to manage transitions: the transition action and the call to update the state machine will be placed into a
     * {@link Callable} and executed in the transactional context.
     *
     * Each service provider may choose a transaction management system that best fits their needs
     *
     * @param callable the {@link Callable} to be execute in the transactional context
     * @param <R> the type of the data returned by the {@link Callable}
     * @return whatever the {@link Callable} returns
     * @throws Exception if anything goes wrong. If anything gets thrown here the transaction must be rolled back
     */
    <R> R runInTransaction(Callable<R> callable) throws Exception;
}
