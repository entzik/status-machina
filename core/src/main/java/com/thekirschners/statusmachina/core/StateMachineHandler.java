package com.thekirschners.statusmachina.core;

import io.liquidshare.statusmachina.core.MachineDef;
import io.liquidshare.statusmachina.core.MachineInstance;
import io.liquidshare.statusmachina.core.TransitionException;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

public interface StateMachineHandler {
    <S,E> MachineInstance<S,E> newMachine(MachineDef<S, E> type);
    <S,E> MachineInstance<S,E> read(MachineDef<S, E> def, String id);
    <S,E> void write(MachineInstance<S, E> instance);
    void lock(String id);
    void release(String id);

    default <S,E> MachineInstance<S,E> instantiate(MachineDef<S, E> type) {
        final MachineInstance<S, E> instance = newMachine(type);
        final String id = instance.getId();
        lock(id);
        return instance;
    }

    default <S,E> MachineInstance<S,E> acquire(MachineDef<S, E> def, String id) {
        lock(id);
        try {
            return read(def, id);
        } catch (Exception e) {
            release(id);
            throw new NoSuchElementException("unable to find state machine with ID " + id);
        }
    }

    default <S,E> void commit(MachineInstance<S, E> instance) {
        try {
            write(instance);
        } catch (Exception e) {
            throw new IllegalStateException("unable to write state machine");
        } finally {
            release(instance.getId());
        }
    }

    default <S,E> String withNewMachine(MachineDef<S, E> type, Consumer<MachineInstance<S, E>> machineLogic) {
        final MachineInstance<S,E> instance = instantiate(type);
        final String id = instance.getId();
        try {
            machineLogic.accept(instance);
            return id;
        } finally {
            commit(instance);
        }
    }

    default <S,E> void withMachine(MachineDef<S, E> def, String id, Consumer<MachineInstance<S, E>> machineLogic) {
        final MachineInstance<S,E> instance = acquire(def, id);
        try {
            machineLogic.accept(instance);
        } finally {
            commit(instance);
        }

    }

    default <S,E> void sendEvent(MachineDef<S, E> def, String id, E event, Consumer<Exception> errorHandler) {
        withMachine(def, id, machineInstance -> {
            try {
                machineInstance.sendEvent(event);
            } catch (TransitionException e) {
                errorHandler.accept(e);
            }
        });
    }

}
