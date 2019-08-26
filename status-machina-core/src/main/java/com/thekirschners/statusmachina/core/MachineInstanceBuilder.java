package com.thekirschners.statusmachina.core;

import java.util.Map;

public class MachineInstanceBuilder<S,E> {
    private MachineDef<S,E> definition;

    public MachineInstanceBuilder(MachineDef<S, E> definition) {
        this.definition = definition;
    }

    public MachineInstance<S,E> withContext(Map<String, String> context) throws TransitionException {
        return new MachineInstance<>(definition, context);
    }
}
