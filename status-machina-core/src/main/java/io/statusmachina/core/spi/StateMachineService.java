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

public interface StateMachineService {
    <S,E> Machine<S,E> newMachine(MachineDefinition<S, E> type, Map<String, String> context) throws TransitionException;
    <S,E> Machine<S,E> newMachine(MachineDefinition<S, E> type, String id, Map<String, String> context) throws TransitionException;
    <S,E> Machine<S,E> read(MachineDefinition<S, E> def, String id) throws TransitionException;
    <S,E> void create(Machine<S, E> instance);
    <S,E> void update(Machine<S, E> instance);
    List<MachineSnapshot> findStale(long seconds);
    List<MachineSnapshot> findFailed();
    List<MachineSnapshot> findTerminated();
}
