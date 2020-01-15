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

package io.statusmachina.core.spi;

import io.statusmachina.core.TransitionException;
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.Machine;
import io.statusmachina.core.api.MachineSnapshot;

import java.util.Map;
import java.util.List;

/**
 * An interface that provides state machine lifecycle services. it allows to create and find state machines.
 * typical implementations will extract some sort of internal state and persist it in a database or distributed cache
 *
 * @param <S> the java type that defines the states of the machine
 * @param <E> the java type that defines events the machine reacts to
 */
public interface StateMachineService<S, E> {
    /**
     * Create a state machine of the specified type and with the specified initial context.
     * <p>
     * Specific implementations may (should) persist the machine as well
     * <p>
     * An ID should be automatically assigned to this machine
     * <p>
     * The machine must automatically execute any STP transitions configured our of the initial state
     *
     * @param type    the machine definition
     * @param context the initial context
     * @return an instance of the machine
     * @throws Exception is thrown if anything goes wrong
     */
    Machine<S, E> newMachine(MachineDefinition<S, E> type, Map<String, String> context) throws Exception;

    /**
     * Create a state machine of the specified type and with the specified initial context.
     * <p>
     * Specific implementations may (should) persist the machine as well
     * <p>
     * The machine must automatically execute any STP transitions configured our of the initial state
     *
     * @param type    the machine definition
     * @param id      an ID the user wants to be assigned to this machine
     * @param context the initial context
     * @return an instance of the machine
     * @throws Exception is thrown if anything goes wrong
     */
    Machine<S, E> newMachine(MachineDefinition<S, E> type, String id, Map<String, String> context) throws Exception;

    /**
     * Locates, restores and returns the state machine of the specified type and with the specified ID
     * <p>
     * The machine must automatically execute any STP transitions configured our of the initial state
     *
     * An exception must be thrown if the ID points to a machine that is not of the specified type
     *
     * @param def the machine definition
     * @param id the id of the machine
     * @return an instance of the machine
     * @throws Exception if anything goes wrong
     */
    Machine<S, E> read(MachineDefinition<S, E> def, String id) throws Exception;

    /**
     * Finds all state machines that have not executed any transitions during in the specified timeout
     *
     * @param seconds - the timeout
     *
     * @return a list of machine descriptions
     */
    List<MachineSnapshot> findStale(long seconds);

    /**
     * finds all state machines stuck in an error state
     * @return a list of machine descriptions
     */
    List<MachineSnapshot> findFailed();

    /**
     * finds all state machines in terminal states
     * @return a list of machine descriptions
     */
    List<MachineSnapshot> findTerminated();
}
