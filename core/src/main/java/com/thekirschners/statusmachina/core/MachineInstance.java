package com.thekirschners.statusmachina.core;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

public class MachineInstance<S, E> {
    final String id;

    private MachineDef<S, E> def;
    private Map<String, String> context;
    private List<TransitionRecord<S,E>> history;
    private Optional<Throwable> error;

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
        error = Optional.empty();

        tryStp();
    }

    public MachineInstance(
            String id,
            MachineDef<S, E> def,
            S currentState,
            Map<String, String> context,
            List<TransitionRecord<S, E>> history,
            Optional<Throwable> error
    ) {
        this.id = id;
        this.def = def;
        this.context = context;
        this.history = history;
        this.error = error;
        this.currentState = currentState;
    }

    public String getId() {
        return id;
    }

    public S getCurrentState() {
        return currentState;
    }

    public Map<String, String> getContext() {
        return Collections.unmodifiableMap(context);
    }

    public List<TransitionRecord<S,E>> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public Optional<Throwable> getError() {
        return error;
    }

    public MachineDef<S, E> getDef() {
        return def;
    }

    public boolean isErrorState() {
        return error.isPresent();
    }

    public void sendEvent(E event) throws TransitionException {
        Transition<S,E> transition = def.findEventTransion(currentState, event).orElseThrow(() -> new IllegalStateException("for machines of type " + def.getName() + " event " + event.toString() + " does not trigger any transition out of state " + currentState.toString()));
        final Optional<Consumer<Map<String, String>>> action = transition.getAction();
        try {
            action.ifPresent(mapConsumer -> mapConsumer.accept(context));
            error = Optional.empty();
        } catch (Throwable t) {
            error = Optional.of(t);
            throw new TransitionException(MachineInstance.this, transition);
        }
        this.currentState = transition.getTo();
        recordEventTransition(event);

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
                recordStpTransition();
                error = Optional.empty();
            } catch (Throwable t) {
                error = Optional.of(t);
                throw new TransitionException(MachineInstance.this, transition);
            }
        }
    }

    private boolean recordEventTransition(E event) {
        return history.add(new TransitionRecord<>(currentState, event, Instant.now()));
    }

    private void recordStpTransition() {
        history.add(new TransitionRecord<>(currentState, Instant.now()));
    }
}
