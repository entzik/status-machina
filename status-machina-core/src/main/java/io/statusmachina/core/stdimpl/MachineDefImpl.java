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

package io.statusmachina.core.stdimpl;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.statusmachina.core.api.ErrorData;
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.MachineDefinitionBuilder;
import io.statusmachina.core.api.Transition;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Defines a state machine by its states, events and transitions
 *
 * @param <S>
 * @param <E>
 */
public class MachineDefImpl<S, E> implements MachineDefinition<S, E> {
    final private ImmutableSet<S> allStates;
    final private S initialState;
    final private ImmutableSet<S> terminalStates;

    final private ImmutableSet<E> events;

    final private ImmutableSet<Transition<S, E>> transitions;
    final private String name;
    final private Function<S, String> stateToString;
    final private Function<String, S> stringToState;
    final private Function<E, String> eventToString;
    final private Function<String, E> stringToEvent;
    private Consumer<ErrorData<S, E>> errorHandler;

    public static <S, E> MachineDefinitionBuilder<S, E> newBuilder() {
        return new BuilderImpl<>();
    }

    public MachineDefImpl(
            String name,
            Set<S> allStates,
            S initialState,
            Set<S> terminalStates,
            Set<E> events,
            Set<Transition<S, E>> transitions,
            Consumer<ErrorData<S, E>> errorHandler,
            Function<S, String> stateToString,
            Function<String, S> stringToState,
            Function<E, String> eventToString,
            Function<String, E> stringToEvent
    ) {
        this.name = name;
        this.errorHandler = errorHandler;
        this.stateToString = stateToString;
        this.stringToState = stringToState;
        this.eventToString = eventToString;
        this.stringToEvent = stringToEvent;
        this.allStates = ImmutableSet.<S>builder().addAll(allStates).build();
        this.initialState = initialState;
        this.terminalStates = ImmutableSet.<S>builder().addAll(terminalStates).build();
        this.events = ImmutableSet.<E>builder().addAll(events).build();
        this.transitions = ImmutableSet.<Transition<S, E>>builder().addAll(transitions).build();
    }

    /**
     * @return the set of states the machine can find itself in
     */
    @Override
    public Set<S> getAllStates() {
        return allStates;
    }

    /**
     * the machine initial state. the state which the machine will find itself in right after instantiations. must be
     * one of the states provided by {@link #getAllStates()}
     *
     * @return the initial state
     */
    @Override
    public S getInitialState() {
        return initialState;
    }

    /**
     * States considered terminal. once in one of these states, the machine can not transition any more. Terminal states
     * must be a subset of the states provided by {@link #getAllStates()}
     *
     * @return terminal states
     */
    @Override
    public Set<S> getTerminalStates() {
        return terminalStates;
    }

    /**
     * @return a list of all events that can cause a transition
     */
    @Override
    public Set<E> getEvents() {
        return events;
    }

    /**
     * @return the list of transitions this machine can execute
     */
    @Override
    public Set<Transition<S, E>> getTransitions() {
        return transitions;
    }

    /**
     * @return a function that converts a state to a string
     */
    @Override
    public Function<S, String> getStateToString() {
        return stateToString;
    }

    /**
     * @return a function that converts a string to a state
     */
    @Override
    public Function<String, S> getStringToState() {
        return stringToState;
    }

    /**
     * @return a function that converts an event to a string
     */
    @Override
    public Function<E, String> getEventToString() {
        return eventToString;
    }

    /**
     * @return a function that converts an event to a string
     */
    @Override
    public Function<String, E> getStringToEvent() {
        return stringToEvent;
    }

    /**
     * find the stp transition configured out of the specified state, if any
     *
     * @param state the state out of which we ae looking for an STP transition
     * @return the stp transition
     */
    @Override
    public Optional<Transition<S, E>> findStpTransition(S state, ImmutableMap<String,String> context) {
        return transitions.stream()
                .filter(t -> t.getFrom().equals(state) && t.isSTP())
                .filter(t -> t.getGuard().map(guard -> guard.apply(context)).orElse(true))
                .findFirst();
    }

    /**
     * finds the transition configured out of the specified state for the specified event, if any
     *
     * @param currentState the state out of which we ae looking for an event transition
     * @param event        the event that triggers the transition
     * @return the traansition
     */
    @Override
    public Optional<Transition<S, E>> findEventTransion(S currentState, E event) {
        final Predicate<Transition<S, E>> fromCurrentState = t -> t.getFrom().equals(currentState);
        final Predicate<Transition<S, E>> forThisEvent = t -> !t.isSTP() && t.getEvent().get().equals(event);
        return transitions.stream().filter(fromCurrentState.and(forThisEvent)).findFirst();
    }

    /**
     * @return a handler to be called when the machine enters an error state (such as failing during a transition)
     */
    @Override
    public Consumer<ErrorData<S, E>> getErrorHandler() {
        return errorHandler;
    }

    /**
     * @return the name of this machine type
     */
    @Override
    public String getName() {
        return name;
    }

    public static class BuilderImpl<S, E> implements MachineDefinitionBuilder<S, E> {
        private Set<S> allStates;
        private S initialState;
        private Set<S> terminalStates;

        private Set<E> events;

        private Set<Transition<S, E>> transitions;

        private String name;

        private Consumer<ErrorData<S, E>> errorHandler;

        private Function<S, String> stateToString;
        private Function<String, S> stringToState;
        private Function<E, String> eventToString;
        private Function<String, E> stringToEvent;


        BuilderImpl() {
        }

        @Override
        public MachineDefinitionBuilder<S, E> name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public MachineDefinitionBuilder<S, E> states(S... allStates) {
            this.allStates = new HashSet<>(Arrays.asList(allStates));
            return this;
        }

        @Override
        public MachineDefinitionBuilder<S, E> initialState(S initialState) {
            if (allStates == null || notAState(initialState))
                throw new IllegalArgumentException("All states must be defined before defining the initial state. The initial state must be selected among previously defined states.");
            else
                this.initialState = initialState;
            return this;
        }

        @Override
        public MachineDefinitionBuilder<S, E> terminalStates(S... terminals) {
            Set<S> terminalStates = new HashSet<>(Arrays.asList(terminals));
            if (allStates == null || !allStates.containsAll(terminalStates))
                throw new IllegalArgumentException("All states must be defined before defining the initial state. Terminal states must be selected among previously defined states.");
            else
                this.terminalStates = terminalStates;
            return this;
        }

        @Override
        public MachineDefinitionBuilder<S, E> events(E... events) {
            this.events = new HashSet<>(Arrays.asList(events));
            return this;
        }

        @Override
        public MachineDefinitionBuilder<S, E> stateToString(Function<S, String> stateToString) {
            this.stateToString = stateToString;
            return this;
        }

        @Override
        public MachineDefinitionBuilder<S, E> stringToState(Function<String, S> stringToState) {
            this.stringToState = stringToState;
            return this;
        }

        @Override
        public MachineDefinitionBuilder<S, E> eventToString(Function<E, String> eventToString) {
            this.eventToString = eventToString;
            return this;
        }

        @Override
        public MachineDefinitionBuilder<S, E> stringToEvent(Function<String, E> stringToEvent) {
            this.stringToEvent = stringToEvent;
            return this;
        }

        @Override
        public MachineDefinitionBuilder<S, E> errorHandler(Consumer<ErrorData<S, E>> errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        @Override
        public MachineDefinitionBuilder<S, E> transitions(Transition<S, E>... allTransitions) {
            Set<Transition<S, E>> transitions = new HashSet<>(Arrays.asList(allTransitions));
            if (events == null || allStates == null)
                throw new IllegalArgumentException("All states and events must be defined before defining transitions. All states except the inital state must be reachable from another state. Each state except terminal states must transition to other states");

            if (!validateTransitions(transitions))
                throw new IllegalStateException("Transitions are not valid");

            this.transitions = transitions;
            return this;
        }

        @Override
        public MachineDefinition<S, E> build() {
            return new MachineDefImpl<S, E>(
                    name,
                    new HashSet<>(allStates),
                    initialState,
                    new HashSet<>(terminalStates),
                    new HashSet<>(events),
                    new HashSet<>(transitions),
                    errorHandler == null ? errorData -> {
                    } : errorHandler,
                    stateToString,
                    stringToState,
                    eventToString,
                    stringToEvent
            );
        }

        private boolean validateTransitions(Set<Transition<S, E>> transitions) {
            for (Transition<S, E> transition : transitions)
                if (notASourcetState(transition.getFrom()) || notADestinationState(transition.getTo()) || notAnEvent(transition.getEvent()))
                    return false;

            return true;
        }

        private boolean notASourcetState(S state) {
            return terminalStates.contains(state) || notAState(state);
        }

        private boolean notADestinationState(S state) {
            return notAState(state);
        }

        private boolean notAState(S state) {
            return !allStates.contains(state);
        }

        private boolean notAnEvent(Optional<E> event) {
            return event.isPresent() && !events.contains(event.get());
        }
    }
}
