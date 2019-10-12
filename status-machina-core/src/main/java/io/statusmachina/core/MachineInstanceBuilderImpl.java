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
