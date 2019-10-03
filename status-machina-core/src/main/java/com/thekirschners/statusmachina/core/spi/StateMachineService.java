package com.thekirschners.statusmachina.core.spi;

import com.thekirschners.statusmachina.core.TransitionException;
import com.thekirschners.statusmachina.core.api.MachineDef;
import com.thekirschners.statusmachina.core.api.MachineInstance;
import com.thekirschners.statusmachina.core.api.MachineSnapshot;

import java.util.Map;
import java.util.List;

public interface StateMachineService {
    <S,E> MachineInstance<S,E> newMachine(MachineDef<S, E> type, Map<String, String> context) throws TransitionException;
    <S,E> MachineInstance<S,E> read(MachineDef<S, E> def, String id) throws TransitionException;
    <S,E> void create(MachineInstance<S, E> instance);
    <S,E> void update(MachineInstance<S, E> instance);
    List<MachineSnapshot> findStale(long minutes);
    List<MachineSnapshot> findFailed();
    List<MachineSnapshot> findTerminated();
}
