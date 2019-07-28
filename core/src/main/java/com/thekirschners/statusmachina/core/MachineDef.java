package com.thekirschners.statusmachina.core;


import java.util.*;
import java.util.function.Predicate;

public class MachineDef<S,E> {
    final private Set<S> allStates;
    final private S initialState;
    final private Set<S> terminalStates;

    final private Set<E> events;

    final private Set<Transition<S,E>> transitions;
    final private String name;

    public static <S,E> Builder<S,E> newBuilder() {
        return new Builder<>();
    }

    public MachineDef(String name, Set<S> allStates, S initialState, Set<S> terminalStates, Set<E> events, Set<Transition<S, E>> transitions) {
        this.name = name;
        this.allStates = allStates;
        this.initialState = initialState;
        this.terminalStates = terminalStates;
        this.events = events;
        this.transitions = transitions;
    }

    public Set<S> getAllStates() {
        return allStates;
    }

    public S getInitialState() {
        return initialState;
    }

    public Set<S> getTerminalStates() {
        return terminalStates;
    }

    public Set<E> getEvents() {
        return events;
    }

    public Set<Transition<S, E>> getTransitions() {
        return transitions;
    }

    public Optional<Transition<S, E>> findStpTransition(S state) {
        return transitions.stream().filter(t -> t.getFrom().equals(state) && t.isSTP()).findFirst();
    }

    public Optional<Transition<S, E>> findEventTransion(S currentState, E event) {
        final Predicate<Transition<S, E>> fromCurrentState = t -> t.getFrom().equals(currentState);
        final Predicate<Transition<S, E>> forThisEvent = t -> !t.isSTP() && t.getEvent().get().equals(event);
        return transitions.stream().filter(fromCurrentState.and(forThisEvent)).findFirst();
    }

    public String getName() {
        return name;
    }

    static class Builder<S,E> {
        private Set<S> allStates;
        private S initialState;
        private Set<S> terminalStates;

        private Set<E> events;

        private Set<Transition<S,E>> transitions;

        private String name;

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

        public Builder<S, E> transitions(Transition<S, E>... allTransitions) {
            Set<Transition<S, E>> transitions = new HashSet<>(Arrays.asList(allTransitions));
            if (events == null || allStates == null)
                throw new IllegalArgumentException("All states and events must be defined before defining transitions. All states except the inital state must be reachable from another state. Each state except terminal states must transition to other states");

            if (!validateTransitions(transitions))
                throw new IllegalStateException("Transitions are not valid");

            this.transitions = transitions;
            return this;
        }

        public MachineDef<S,E> build() {
            return new MachineDef<>(
                    name,
                    Collections.unmodifiableSet(allStates),
                    initialState,
                    Collections.unmodifiableSet(terminalStates),
                    Collections.unmodifiableSet(events),
                    Collections.unmodifiableSet(transitions)
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
