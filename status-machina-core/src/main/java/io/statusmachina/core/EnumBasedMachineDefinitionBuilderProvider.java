/*
 *  Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */

package io.statusmachina.core;

import io.statusmachina.core.api.MachineDefinitionBuilder;
import io.statusmachina.core.api.MachineDefinitionBuilderProvider;

public class EnumBasedMachineDefinitionBuilderProvider<S extends Enum<S>,E extends Enum<E>> implements MachineDefinitionBuilderProvider<S,E> {
    @Override
    public MachineDefinitionBuilder<S, E> getMachineDefinitionBuilder(Class<S> stateType, Class<E> eventType) {
        return new MachineDefImpl.BuilderImpl<S,E>()
                .stringToEvent(eventString -> Enum.valueOf(eventType, eventString))
                .eventToString(Enum::name)
                .stringToState(stateString -> Enum.valueOf(stateType, stateString))
                .stateToString(Enum::name);
    }
}
