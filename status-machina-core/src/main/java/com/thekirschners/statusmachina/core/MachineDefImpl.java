package com.thekirschners.statusmachina.core;


import com.thekirschners.statusmachina.core.api.MachineDef;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Defines a state machine by its states, events and transitions
 *
 * @param <S>
 * @param <E>
 */
public class MachineDefImpl<S,E> implements MachineDef<S, E> {
    /**
     * the type of the object used to define the machine's state
     */
    private Class<S> stateType;
    /**
     * the type of the events received by the state machine
     */
    private Class<E> eventType;
    final private Set<S> allStates;
    final private S initialState;
    final private Set<S> terminalStates;

    final private Set<E> events;

    final private Set<Transition<S,E>> transitions;
    final private String name;
    final private Function<S, String> stateToString;
    final private Function<String, S> stringToState;
    final private Function<E, String> eventToString;
    final private Function<String, E> stringToEvent;

    public static <S,E> Builder<S,E> newBuilder() {
        return new Builder<>();
    }

    public MachineDefImpl(
            String name,
            Set<S> allStates,
            S initialState,
            Set<S> terminalStates,
            Set<E> events,
            Set<Transition<S, E>> transitions,
            Function<S,String> stateToString,
            Function<String,S> stringToState,
            Function<E, String> eventToString,
            Function<String, E> stringToEvent
    ) {
        this.name = name;
        this.stateToString = stateToString;
        this.stringToState = stringToState;
        this.eventToString = eventToString;
        this.stringToEvent = stringToEvent;
        this.stateType = stateType;
        this.eventType = eventType;
        this.allStates = allStates;
        this.initialState = initialState;
        this.terminalStates = terminalStates;
        this.events = events;
        this.transitions = transitions;
    }

    @Override
    public Set<S> getAllStates() {
        return allStates;
    }

    @Override
    public S getInitialState() {
        return initialState;
    }

    @Override
    public Set<S> getTerminalStates() {
        return terminalStates;
    }

    @Override
    public Set<E> getEvents() {
        return events;
    }

    @Override
    public Set<Transition<S, E>> getTransitions() {
        return transitions;
    }

    @Override
    public Function<S, String> getStateToString() {
        return stateToString;
    }

    @Override
    public Function<String, S> getStringToState() {
        return stringToState;
    }

    @Override
    public Function<E, String> getEventToString() {
        return eventToString;
    }

    @Override
    public Function<String, E> getStringToEvent() {
        return stringToEvent;
    }

    @Override
    public Optional<Transition<S, E>> findStpTransition(S state) {
        return transitions.stream().filter(t -> t.getFrom().equals(state) && t.isSTP()).findFirst();
    }

    @Override
    public Optional<Transition<S, E>> findEventTransion(S currentState, E event) {
        final Predicate<Transition<S, E>> fromCurrentState = t -> t.getFrom().equals(currentState);
        final Predicate<Transition<S, E>> forThisEvent = t -> !t.isSTP() && t.getEvent().get().equals(event);
        return transitions.stream().filter(fromCurrentState.and(forThisEvent)).findFirst();
    }

    @Override
    public String getName() {
        return name;
    }

    public static class Builder<S,E> {
        private Set<S> allStates;
        private S initialState;
        private Set<S> terminalStates;

        private Set<E> events;

        private Set<Transition<S,E>> transitions;

        private String name;

        Function<S,String> stateToString;
        Function<String,S> stringToState;
        Function<E, String> eventToString;
        Function<String, E> stringToEvent;


        Builder() {
        }

        public Builder<S, E> setName(String name) {
            this.name = name;
            return this;
        }

        public Builder<S, E> states(S... allStates) {
            this.allStates = new HashSet<>(Arrays.asList(allStates));
            return this;
        }

        public Builder<S, E> initialState(S initialState) {
            if (allStates == null || notAState(initialState))
                throw new IllegalArgumentException("All states must be defined before defining the initial state. The initial state must be selected among previously defined states.");
            else
                this.initialState = initialState;
            return this;
        }

        public Builder<S, E> terminalStates(S... terminals) {
            Set<S> terminalStates = new HashSet<>(Arrays.asList(terminals));
            if (allStates == null || !allStates.containsAll(terminalStates))
                throw new IllegalArgumentException("All states must be defined before defining the initial state. Terminal states must be selected among previously defined states.");
            else
                this.terminalStates = terminalStates;
            return this;
        }

        public Builder<S, E> events(E... events) {
            this.events = new HashSet<>(Arrays.asList(events));
            return this;
        }

        public Builder<S, E> setStateToString(Function<S, String> stateToString) {
            this.stateToString = stateToString;
            return this;
        }

        public Builder<S, E> setStringToState(Function<String, S> stringToState) {
            this.stringToState = stringToState;
            return this;
        }

        public Builder<S, E> setEventToString(Function<E, String> eventToString) {
            this.eventToString = eventToString;
            return this;
        }

        public Builder<S, E> setStringToEvent(Function<String, E> stringToEvent) {
            this.stringToEvent = stringToEvent;
            return this;
        }

        public Builder<S, E> transitions(Transition<S, E>... allTransitions) {
            Set<Transition<S, E>> transitions = new HashSet<>(Arrays.asList(allTransitions));
            if (events == null || allStates == null)
                throw new IllegalArgumentException("All states and events must be defined before defining transitions. All states except the inital state must be reachable from another state. Each state except terminal states must transition to other states");

            if (!validateTransitions(transitions))
                throw new IllegalStateException("Transitions are not valid");

            this.transitions = transitions;
            return this;
        }

        public MachineDefImpl<S,E> build() {
            return new MachineDefImpl<>(
                    name,
                    new HashSet<>(allStates),
                    initialState,
                    new HashSet<>(terminalStates),
                    new HashSet<>(events),
                    new HashSet<>(transitions),
                    stateToString,
                    stringToState,
                    eventToString,
                    stringToEvent
            );
        }

        private boolean validateTransitions(Set<Transition<S, E>> transitions) {
            for (Transition<S,E> transition : transitions)
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
