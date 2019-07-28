package com.thekirschners.statusmachina.core;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

public class MachineInstance<S, E> {
    final String id;

    private MachineDef<S, E> def;
    private Map<String, String> context;
    private List<TransitionResult<S,E>> history;

    private S currentState;

    public static <S, E> MachineInstanceBuilder<S, E> ofType(MachineDef<S, E> definition) {
        return new MachineInstanceBuilder<>(definition);
    }

    MachineInstance(MachineDef<S, E> def, Map<String, String> context) throws TransitionException {
        this.id = UUID.randomUUID().toString();
        this.def = def;
        this.context = context;
        this.history = new ArrayList<>();

        currentState = def.getInitialState();
        recordStpTransition();

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
        recordEventTransition(event);

        tryStp();
    }

    public S getCurrentState() {
        return currentState;
    }

    private void tryStp() throws TransitionException {
        Optional<Transition<S, E>> stpTransition;
        while ((stpTransition = def.findStpTransition(currentState)).isPresent()) {
            final Transition<S, E> transition = stpTransition.get();
            final Optional<Consumer<Map<String, String>>> action = transition.getAction();
            try {
                action.ifPresent(mapConsumer -> mapConsumer.accept(context));
                currentState = transition.getTo();
                recordStpTransition();
            } catch (Throwable t) {
                throw new TransitionException(MachineInstance.this, transition);
            }
        }
    }

    private boolean recordEventTransition(E event) {
        return history.add(new TransitionResult<>(currentState, event, Instant.now()));
    }

    private void recordStpTransition() {
        history.add(new TransitionResult<>(currentState, Instant.now()));
    }
}
