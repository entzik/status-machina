package com.thekirschners.statusmachina.core;

public class TransitionException extends Exception {
    private MachineInstanceImpl machineInstance;
    private Transition<?, ?> transition;

    public <S,E> TransitionException(MachineInstanceImpl<S, E> machineInstance, Transition<S, E> transition) {
        this.machineInstance = machineInstance;
        this.transition = transition;
    }
}
