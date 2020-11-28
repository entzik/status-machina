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

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p><strong>Defines a state machine</strong> in terms of states, events and transitions.</p>
 *
 * <p><strong>States</strong> are of a configurable type. The definition must provide all states, including exactly one initial state and at least one terminal state.
 * The inital state cannot be a terminal state.</p>
 *
 *<p><strong>Transitions</strong> determine how a machine will translate from one state to the other. There are two types of transitions:
 * <ul>
 *     <li>STP (straight through processing</li>
 *     <li>Event based</li>
 * </ul>
 * </p>
 * <p>
 * An STP transition configured from state A to state B triggers an automatic translation to state B as soon as the machine enters state A.
 * In the current version there can only be one STP transition configured from any state.
 * </p>
 * <p>
 * An event transition from state A to state B, on event E will cause the machine in state A to translate to state B when it reveives event E
 * </p>
 *
 * <p>Events are passive items delivered the machine reacts to</p>
 *
 * @param <S> - the type of the states the machine can find itself in
 * @param <E> - the type of events the machine can react to
 */
public interface MachineDefinition<S, E> {
    /**
     * @return all possible states the machine can find itself in. This must inclue the initial and terminal states
     */
    Set<S> getAllStates();

    /**
     * @return the state a machine of this type will find itself in when first initialized.
     */
    S getInitialState();

    /**
     * Idle states are states on which the machine is expected to pause for a long period of time, but are not final
     * states. Idle states are useful when modelling perpetual or very long lived state machines. They allow the machine
     * to pause in an idle state without the machine being picked up as stale by monitoring tools
     *
     * @return a set of idle states.
     */
    Set<S> getIdleStates();

    /**
     * @return set of terminal states. Once the machine reaches one of this states it is considered to be completed. it is not possible to
     * configure transitions out of a terminal state
     */
    Set<S> getTerminalStates();

    /**
     * @return the set of event the machine can react to
     */
    Set<E> getEvents();

    /**
     * @return the set of transitions that governs the state machine
     */
    Set<Transition<S, E>> getTransitions();

    /**
     * @param state the state out of which we ae looking for an STP transition
     * @return an STP transition, if any
     */
    Optional<Transition<S, E>> findStpTransition(S state, ImmutableMap<String, String> context);

    /**
     * @param state the state out of which we ae looking for a transition that matches the event
     * @param event the event that triggers the transition
     * @return an STP transition, if any
     */
    Optional<Transition<S, E>> findEventTransition(S state, E event);

    /**
     * Provides a handler to be invoked when the machine enters an error state. An error state is reached when an
     * error occurs during a transition
     *
     * @return the error handler
     */
    Consumer<ErrorData<S,E>> getErrorHandler();

    /**
     * Provides a handler to be invoked when the machine finish a no stp transition
     *
     * @return the error handler
     */
    Consumer<TransitionData<S,E>> getTransitionHandler();

    /**
     * @return the name of this state machine
     */
    String getName();

    /**
     * @return a handler that converts a state into its string representation
     */
    Function<S, String> getStateToString();

    /**
     * @return a handler that converts a state's string representation into the state itself
     */
    Function<String, S> getStringToState();

    /**
     * @return a handler that converts an event into its string representation
     */
    Function<E, String> getEventToString();

    /**
     * @return a handler that convers an event's string representation into the event itself
     */
    Function<String, E> getStringToEvent();
}
