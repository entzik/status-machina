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

package io.statusmachina.core.stdimpl;

import com.google.common.collect.ImmutableMap;
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.Machine;
import io.statusmachina.core.api.MachineBuilder;
import io.statusmachina.core.spi.MachinePersistenceCallback;

import java.util.Map;
import java.util.concurrent.Callable;

public class MachineInstanceBuilderImpl<S,E> implements MachineBuilder<S,E> {
    private MachineDefinition<S,E> definition;
    private ImmutableMap<String, String> context;
    private String id;
    private  MachinePersistenceCallback<S,E> machinePersistenceCallback;


    @Override
    public MachineBuilder<S,E> ofType(MachineDefinition<S, E> definition) {
        this.definition = definition;
        return this;
    }

    @Override
    public MachineBuilder<S,E> withContext(Map<String, String> context) {
        this.context = ImmutableMap.<String, String>builder().putAll(context).build();
        return this;
    }

    @Override
    public MachineBuilder<S,E> withId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public MachineBuilder<S,E> withPersistence(MachinePersistenceCallback<S,E> machinePersistenceCallback) {
        this.machinePersistenceCallback = machinePersistenceCallback;
        return this;
    }

    @Override
    public Machine<S,E> build() throws Exception {
        if (definition == null)
            throw new IllegalStateException("a state machine definition must be provided in order for a state machine instance to be built");
        if (context == null)
            throw new IllegalStateException("a context must be provided in order for a state machine instance to be built");
        if (machinePersistenceCallback == null)
            machinePersistenceCallback = new MachinePersistenceCallback<S, E>() {
                @Override
                public Machine<S, E> saveNew(Machine<S, E> machine) {
                    return machine;
                }

                @Override
                public Machine<S, E> update(Machine<S, E> machine) {
                    return machine;
                }

                @Override
                public <R> R runInTransaction(Callable<R> callable) throws Exception {
                    return callable.call();
                }
            };
        if (id == null) {
            return new MachineInstanceImpl<S,E>(definition, machinePersistenceCallback, context);
        } else
            return new MachineInstanceImpl<S,E>(definition, id, machinePersistenceCallback, context);
    }
}
