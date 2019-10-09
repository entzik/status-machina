package io.statusmachina.core.api;

import java.util.Map;
import java.util.Optional;

/**
 * data structure that describes the context in which an error occurred
 * @param <S> the type of the state machine's state
 * @param <E> the type of event the state machine accepts
 */
public interface ErrorData<S, E> {
    /**
     * @return the state from which the machine was transitioning out
     */
    S getFrom();

    /**
     * @return the state to which the machine was transitioning to
     */
    S getTo();

    /**
     * @return the event that triggered the transition, empty optional if STP
     */
    Optional<E> getEvent();

    /**
     * @return the context as it was in the source state
     */
    Map<String, String> getContext();

    /**
     * @param <P> the type of the parameter
     * @return the parameter passed along with the event
     */
    <P> P getEventParameter();

    /**
     * @return the error message
     */
    String getErrorMessage();
}
