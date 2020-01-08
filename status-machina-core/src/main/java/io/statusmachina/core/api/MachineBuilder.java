/*
 *
 * Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package io.statusmachina.core.api;

import com.google.common.collect.ImmutableList;
import io.statusmachina.core.MachineInstanceImpl;
import io.statusmachina.core.TransitionException;
import io.statusmachina.core.spi.MachinePersistenceCallback;

import java.util.Map;
import java.util.function.Consumer;

/**
 * a contract that defines a state machine builder
 */
public interface MachineBuilder<S,E> {
    /**
     * specify a state machine definition. it is mandatory in order to be able to build a state machine.
     * @param definition the state machine definition
     * @param <S> the type of machine state
     * @param <E> the type of events accepted by the machine
     * @return the updated machine builder
     */
    MachineBuilder<S,E> ofType(MachineDefinition<S,E> definition);

    /**
     * specify the initial context of the state machine you are building
     * @param context context entries
     * @param <S> the type of machine state
     * @param <E> the type of events accepted by the machine
     * @return the updated machine builder
     */
    MachineBuilder<S,E> withContext(Map<String, String> context);

    /**
     * specify an machine ID beforehand. calling this method is optional, it not called a UUID will be generated on the fly
     * @param id the ID to be used for the machine
     * @param <S> the type of machine state
     * @param <E> the type of events accepted by the machine
     * @return the updated machine builder
     */
    MachineBuilder<S,E> withId(String id);

    /**
     * configure a set of callbacks to be invoked after each transition - typically to implement persistence or audit trails
     * @param callbacks callbacks to be configured
     * @param <S> the type of machine state
     * @param <E> the type of events accepted by the machine
     * @return the updated machine builder
     */
    MachineBuilder<S,E> withPersistence(MachinePersistenceCallback<S,E> machinePersistenceCallback);

    /**
     * build a state machine machine off the specified type and context, and put it in the defined initial state.
     * IF STP transitions are definied out of the initial state, they will be executed.
     *
     * @param <S> the type of machine state
     * @param <E> the type of events accepted by the machine
     * @return a state machine instance
     * @throws TransitionException if transiton imtegrity rules have failed
     */
    Machine<S,E> build() throws Exception;

}
