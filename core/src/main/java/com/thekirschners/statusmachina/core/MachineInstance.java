package com.thekirschners.statusmachina.core;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class MachineInstance<S, E> {
    final String id;

    private MachineDef<S, E> def;
    private Map<String, String> context;

    private S currentState;

    public static <S, E> MachineInstanceBuilder<S, E> ofType(MachineDef<S, E> definition) {
        return new MachineInstanceBuilder<>(definition);
    }

    MachineInstance(MachineDef<S, E> def, Map<String, String> context) throws TransitionException {
        this.id = UUID.randomUUID().toString();
        this.def = def;
        this.context = context;

        currentState = def.getInitialState();

        tryStp();
    }

    public String getId() {
        return id;
    }

    public void sendEvent(E event) throws TransitionException {
        Transition<S,E> transition = def.findEventTransion(currentState, event).orElseThrow(() -> new IllegalStateException("for machines of type " + def.getName() + " event " + event.toString() + " does not trigger any transition out of state " + currentState.toString()));
        final Optional<Consumer<Map<String, String>>> action = transition.getAction();
        try {
            action.ifPresent(mapConsumer -> mapConsumer.accept(context));
        } catch (Throwable t) {
            throw new TransitionException(MachineInstance.this, transition);
        }
        this.currentState = transition.getTo();

        tryStp();
    }

    private void tryStp() throws TransitionException {
        Optional<Transition<S, E>> stpTransition;
        while ((stpTransition = def.findStpTransition(currentState)).isPresent()) {
            final Transition<S, E> transition = stpTransition.get();
            final Optional<Consumer<Map<String, String>>> action = transition.getAction();
            try {
                action.ifPresent(mapConsumer -> mapConsumer.accept(context));
                currentState = transition.getTo();
            } catch (Throwable t) {
                throw new TransitionException(MachineInstance.this, transition);
            }
        }
    }

    public S getCurrentState() {
        return currentState;
    }
}
