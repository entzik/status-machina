package io.statusmachina.core.spi;

import io.statusmachina.core.TransitionException;
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.Machine;
import io.statusmachina.core.api.MachineSnapshot;

import java.util.Map;
import java.util.List;

public interface StateMachineService {
    <S,E> Machine<S,E> newMachine(MachineDefinition<S, E> type, Map<String, String> context) throws TransitionException;
    <S,E> Machine<S,E> read(MachineDefinition<S, E> def, String id) throws TransitionException;
    <S,E> void create(Machine<S, E> instance);
    <S,E> void update(Machine<S, E> instance);
    List<MachineSnapshot> findStale(long seconds);
    List<MachineSnapshot> findFailed();
    List<MachineSnapshot> findTerminated();
}
