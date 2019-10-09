package io.statusmachina.core.api;

import io.statusmachina.core.TransitionException;

import java.util.Map;

/**
 * a contract that defines a state machine builder
 */
public interface MachineBuilder {
    /**
     * specify a state machine definition. it is mandatory in order to be able to build a state machine.
     * @param definition the state machine definition
     * @param <S> the type of machine state
     * @param <E> the type of events accepted by the machine
     * @return the updated machine builder
     */
    <S,E> MachineBuilder ofType(MachineDefinition<S,E> definition);

    /**
     * specify the initial context of the state machine you are building
     * @param context context entries
     * @param <S> the type of machine state
     * @param <E> the type of events accepted by the machine
     * @return the updated machine builder
     */
    <S,E> MachineBuilder withContext(Map<String, String> context);

    /**
     * build a state machine machine off the specified type and context, and put it in the defined initial state.
     * IF STP transitions are definied out of the initial state, they will be executed.
     *
     * @param <S> the type of machine state
     * @param <E> the type of events accepted by the machine
     * @return a state machine instance
     * @throws TransitionException if transiton imtegrity rules have failed
     */
    <S,E> Machine<S,E> build() throws TransitionException;
}
