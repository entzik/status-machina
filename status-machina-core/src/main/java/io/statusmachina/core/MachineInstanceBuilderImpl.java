/*
 *
 *  * Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *
 */

package io.statusmachina.core;

import com.google.common.collect.ImmutableMap;
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.Machine;
import io.statusmachina.core.api.MachineBuilder;

import java.util.Map;

public class MachineInstanceBuilderImpl implements MachineBuilder {
    private MachineDefinition<?,?> definition;
    private ImmutableMap<String, String> context;

    @Override
    public <S,E> MachineBuilder ofType(MachineDefinition<S, E> definition) {
        this.definition = definition;
        return this;
    }

    @Override
    public <S,E> MachineBuilder withContext(Map<String, String> context) {
        this.context = ImmutableMap.<String, String>builder().putAll(context).build();
        return this;
    }

    @Override
    public <S,E> Machine<S,E> build() throws TransitionException {
        if (definition == null)
            throw new IllegalStateException("a state machine definition must be provided in order for a state machine instance to be built");
        if (context == null)
            throw new IllegalStateException("a context must be provided in order for a state machine instance to be built");
        return new MachineInstanceImpl<S,E>((MachineDefinition<S, E>) definition, context);
    }
}
