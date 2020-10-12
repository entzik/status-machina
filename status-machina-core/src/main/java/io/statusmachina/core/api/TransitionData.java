package io.statusmachina.core.api;

import java.util.Map;
import java.util.Optional;

public interface TransitionData<S, E> extends StateMachineData{
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
}
