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

package io.statusmachina.core.api;

import io.statusmachina.core.Transition;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <strong>Provides all data required to configure and build state machine definition.</strong>
 *
 * @param <S>
 * @param <E>
 */
public interface MachineDefinitionBuilder<S, E> {
    /**
     * configures a name for the state machine
     *
     * @param name the state machine name
     * @return an instance of the machine definition builder configured with the state machine name
     */
    MachineDefinitionBuilder<S, E> name(String name);

    /**
     * configures the state machine states. these are all the states a state machine built off this definition could
     * potentially find itself in
     *
     * @param allStates the state machine states
     * @return an instance of the machine definition builder configured with the state machine states
     */
    MachineDefinitionBuilder<S, E> states(S... allStates);

    /**
     * configures the state machine's initial state. this is the state in which a state machine build off this definition
     * will find itself in immediately being instantiated. the initial state must be one of the states returned
     * by {@link #states(Object[])}
     *
     * @param initialState the state machine initial state
     * @return an instance of the machine definition builder configured with the state machine initial state
     */
    MachineDefinitionBuilder<S, E> initialState(S initialState);

    /**
     * configures the state machine's terminal states. when the state machine arrived in one of these states no
     * further transitions are possible. the terminal states must be a subset of the states returned
     * by {@link #states(Object[])}
     *
     * @param terminalStates the state machine initial state
     * @return an instance of the machine definition builder configured with the state machine's terminal states
     */
    MachineDefinitionBuilder<S, E> terminalStates(S... terminalStates);

    /**
     * configures the set of events the state machine can react to. sending such an event to the machine can trigger
     * a transition
     *
     * @param events the set of events to which the machine can react
     * @return an instance of the machine definition builder configured with the events the state machine can react to
     */
    MachineDefinitionBuilder<S, E> events(E... events);

    /**
     * configures a function that converts a state into a string. used for serialization and persistence purposes.
     *
     * @param stateToString the conversion function
     * @return an instance of the machine definition builder configured with the state to string conversion function
     */
    MachineDefinitionBuilder<S, E> stateToString(Function<S, String> stateToString);

    /**
     * configures a function that converts a string into a state. used for serialization and persistence purposes.
     *
     * @param stringToState the conversion function
     * @return an instance of the machine definition builder configured with the string to state conversion function
     */
    MachineDefinitionBuilder<S, E> stringToState(Function<String, S> stringToState);

    /**
     * configures a function that converts an event into a string. used for serialization and persistence purposes.
     *
     * @param eventToString the conversion function
     * @return an instance of the machine definition builder configured with the event to string conversion function
     */
    MachineDefinitionBuilder<S, E> eventToString(Function<E, String> eventToString);

    /**
     * configures a function that converts an string into an event. used for serialization and persistence purposes.
     *
     * @param stringToEvent the conversion function
     * @return an instance of the machine definition builder configured with the string to event conversion function
     */
    MachineDefinitionBuilder<S, E> stringToEvent(Function<String, E> stringToEvent);

    /**
     * configures a callback that will be invoked when the machine enters an error state
     *
     * @param errorHandler the callbacl
     * @return an instance of the machine definition builder configured with the error handling callback
     */
    MachineDefinitionBuilder<S, E> errorHandler(Consumer<ErrorData<S, E>> errorHandler);

    /**
     * configures the transitions that define how the state machines moves from one state to another either in reaction
     * to events being received or through STP (straight through processing) when conditions are met
     * <p>
     * the following constraints must be enforced by implementation of this interface
     * <ul>
     * <li>no transition can target the initial state</li>
     * <li>no transitions can be configured out of any terminal state</li>
     * <li>identical transitions should not be allowd </li>
     * <li>only one stp transition is allowed out of one state (until transition guards will be specified and implemented)</li>
     * </ul>
     *
     * @param allTransitions the set of transitions
     * @return an instance of the machine definition builder configured with the transition set
     */
    MachineDefinitionBuilder<S, E> transitions(Transition<S, E>... allTransitions);

    /**
     * builds the state machine definition and enforces all integrity concerns specified above. an exception should be
     * thrown should any of the integrity checks fail
     *
     * @return a state machine definition as configured.
     */
    MachineDefinition<S, E> build();
}
