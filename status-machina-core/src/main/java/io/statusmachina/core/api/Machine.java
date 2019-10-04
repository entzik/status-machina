package io.statusmachina.core.api;

import io.statusmachina.core.TransitionException;
import io.statusmachina.core.TransitionRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Machine<S, E> {
    String getId();

    S getCurrentState();

    Map<String, String> getContext();

    List<TransitionRecord<S, E>> getHistory();

    Optional<String> getError();

    MachineDefinition<S, E> getDef();

    boolean isErrorState();

    void sendEvent(E event) throws TransitionException;

    <P> void sendEvent(E event, P param) throws TransitionException;

    void recoverFromError(S state, Map<String, String> context);

    Machine<S,E> deepClone() throws TransitionException;

    Machine<S,E> setStateVersion(long version);

    long getVersion();

    boolean isTerminalState();
}
