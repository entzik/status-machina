package io.statusmachina.core.api;

import io.statusmachina.core.TransitionException;

import java.util.Map;

public interface MachineBuilder {
    <S,E> MachineBuilder ofType(MachineDefinition<S,E> definition);
    <S,E> MachineBuilder withContext(Map<String, String> context);
    <S,E> Machine<S,E> build() throws TransitionException;
}
