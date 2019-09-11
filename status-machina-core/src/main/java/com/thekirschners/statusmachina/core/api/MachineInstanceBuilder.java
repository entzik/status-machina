package com.thekirschners.statusmachina.core.api;

import com.thekirschners.statusmachina.core.TransitionException;

import java.util.Map;

public interface MachineInstanceBuilder {
    <S,E> MachineInstanceBuilder ofType(MachineDef<S,E> definition);
    <S,E> MachineInstanceBuilder withContext(Map<String, String> context);
    <S,E> MachineInstance<S,E> build() throws TransitionException;
}
