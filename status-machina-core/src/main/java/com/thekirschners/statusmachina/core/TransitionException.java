package com.thekirschners.statusmachina.core;

public class TransitionException extends Exception {
    private MachineInstance machineInstance;
    private Transition<?, ?> transition;

    public <S,E> TransitionException(MachineInstance<S, E> machineInstance, Transition<S, E> transition) {
        this.machineInstance = machineInstance;
        this.transition = transition;
    }
}
