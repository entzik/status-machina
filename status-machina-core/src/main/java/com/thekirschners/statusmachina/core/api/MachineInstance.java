package com.thekirschners.statusmachina.core.api;

import com.thekirschners.statusmachina.core.TransitionException;
import com.thekirschners.statusmachina.core.TransitionRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MachineInstance<S, E> {
    String getId();

    S getCurrentState();

    Map<String, String> getContext();

    List<TransitionRecord<S, E>> getHistory();

    Optional<Throwable> getError();

    MachineDef<S, E> getDef();

    boolean isErrorState();

    void sendEvent(E event) throws TransitionException;
}
