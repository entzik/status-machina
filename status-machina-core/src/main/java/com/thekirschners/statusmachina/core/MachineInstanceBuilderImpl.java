package com.thekirschners.statusmachina.core;

import com.thekirschners.statusmachina.core.api.MachineDef;
import com.thekirschners.statusmachina.core.api.MachineInstance;
import com.thekirschners.statusmachina.core.api.MachineInstanceBuilder;

import java.util.Map;

public class MachineInstanceBuilderImpl implements MachineInstanceBuilder {
    private MachineDef<?,?> definition;
    private Map<String, String> context;

    @Override
    public <S,E> MachineInstanceBuilder ofType(MachineDef<S, E> definition) {
        this.definition = definition;
        return this;
    }

    @Override
    public <S,E> MachineInstanceBuilder withContext(Map<String, String> context) {
        this.context = context;
        return this;
    }

    @Override
    public <S,E> MachineInstance<S,E> build() throws TransitionException {
        if (definition == null)
            throw new IllegalStateException("a state machine definition must be provided in order for a state machine instance to be built");
        if (context == null)
            throw new IllegalStateException("a context must be provided in order for a state machine instance to be built");
        return new MachineInstanceImpl<S,E>((MachineDef<S, E>) definition, context);
    }
}
