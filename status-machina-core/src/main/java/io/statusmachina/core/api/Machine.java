/*
 *
 * Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package io.statusmachina.core.api;

import com.google.common.collect.ImmutableMap;
import io.statusmachina.core.stdimpl.TransitionRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * defines a state machine
 *
 * @param <S> the type of the machine's state
 * @param <E> the type of events the machine receives
 */
public interface Machine<S, E> {
    /**
     * @return the machine's unique ID
     */
    String getId();

    /**
     * @return the state the machine is currently in
     */
    S getCurrentState();

    /**
     * @return the machine's context
     */
    ImmutableMap<String, String> getContext();

    /**
     * @return the history of transitions executed on this state machine since it was created, to present
     */
    List<TransitionRecord<S, E>> getHistory();

    /**
     * @return if the machine is in an error state, it returns the description. otherwise an empty optional
     */
    ErrorType getErrorType();

    /**
     * @return if the machine is in an error state, it returns the description. otherwise an empty optional
     */
    Optional<String> getError();

    /**
     * returns this state machine's definition
     */
    MachineDefinition<S, E> getDefinition();

    /**
     * @return true if the machine is in an error state, false otherwise
     */
    boolean isErrorState();

    /**
     * returns true if the current state is an idle state. see {@link MachineDefinition#getIdleStates()}
     * @return true if the current state is an idle state, false if not
     */
    boolean isIdleState();

    /**
     * returns true if the current state is a terminal one
     */
    boolean isTerminalState();

    /**
     * starts the state machine. if there is any valid STP transition out of the initial state, it will be executed
     * @return
     */
    Machine<S, E> start();

    /**
     * attempts to trigger stp transitions out of the current state
     * @return the same machine if no stp transitions are configured out of the current state, otherwise a machine in the state at the end of the stp transitions chain
     */
    Machine<S, E> resume();

    /**
     * delivers an event to the state machine.
     *
     * @param event the event to be delivered
     *
     * @throws TransitionException if there is no transition configured for this event out of the
     * current state or if an error occurs during the transition. this could happen while executing the action
     * associated with the transition.
     */
    Machine<S,E> sendEvent(E event) throws TransitionException;

    /**
     * delivers an event to the state machine.
     *
     * @param event the event to be delivered
     * @param param a parameter that gives context to the event. the parameter will be passed on to the action associated
     *              with the transition,
     *
     * @throws TransitionException if there is no transition configured for this event out of the
     * current state or if an error occurs during the transition. An error could occur when while executing the action
     * associated with the transition.
     */
    <P> Machine<S,E> sendEvent(E event, P param) throws TransitionException;

    /**
     * resets the machine to a particular state and context if it currently is in an error state. If this method is
     * called while the machine is not in an error state, {@link IllegalStateException} should be thrown.
     *
     * the provided context will completely
     *
     * @param state the state the machine will transition to
     * @param context the context to be applied
     */
    Machine<S,E> recoverFromError(S state, Map<String, String> context);
}
