package com.thekirschners.statusmachina.core.api;

import com.thekirschners.statusmachina.core.Transition;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public interface MachineDef<S, E> {
    Set<S> getAllStates();

    S getInitialState();

    Set<S> getTerminalStates();

    Set<E> getEvents();

    Set<Transition<S, E>> getTransitions();

    Optional<Transition<S, E>> findStpTransition(S state);

    Optional<Transition<S, E>> findEventTransion(S currentState, E event);

    Consumer<ErrorData<S,E>> getErrorHandler();

    String getName();

    Function<S, String> getStateToString();

    Function<String, S> getStringToState();

    Function<E, String> getEventToString();

    Function<String, E> getStringToEvent();
}
