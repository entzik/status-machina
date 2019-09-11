package com.thekirschners.statusmachina.core;

import com.thekirschners.statusmachina.core.api.MachineDef;
import com.thekirschners.statusmachina.core.api.MachineInstance;
import com.thekirschners.statusmachina.core.api.MachineInstanceBuilder;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

public class MachineInstanceImpl<S, E> implements MachineInstance<S, E> {
    final String id;

    private MachineDef<S, E> def;
    private Map<String, String> context;
    private List<TransitionRecord<S,E>> history;
    private Optional<Throwable> error;

    private S currentState;

    public static <S, E> MachineInstanceBuilder ofType(MachineDef<S, E> definition) {
        return new MachineInstanceBuilderImpl().ofType(definition);
    }

    MachineInstanceImpl(MachineDef<S, E> def, Map<String, String> context) throws TransitionException {
        this.id = UUID.randomUUID().toString();
        this.def = def;
        this.context = context;
        this.history = new ArrayList<>();

        currentState = def.getInitialState();
        recordStpTransition();
        error = Optional.empty();

        tryStp();
    }

    public MachineInstanceImpl(
            String id,
            MachineDef<S, E> def,
            S currentState,
            Map<String, String> context,
            List<TransitionRecord<S, E>> history,
            Optional<Throwable> error
    ) throws TransitionException {
        this.id = id;
        this.def = def;
        this.context = context;
        this.history = history;
        this.error = error;
        this.currentState = currentState;

        tryStp();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public S getCurrentState() {
        return currentState;
    }

    @Override
    public Map<String, String> getContext() {
        return Collections.unmodifiableMap(context);
    }

    @Override
    public List<TransitionRecord<S,E>> getHistory() {
        return Collections.unmodifiableList(history);
    }

    @Override
    public Optional<Throwable> getError() {
        return error;
    }

    @Override
    public MachineDef<S, E> getDef() {
        return def;
    }

    @Override
    public boolean isErrorState() {
        return error.isPresent();
    }

    @Override
    public void sendEvent(E event) throws TransitionException {
        Transition<S,E> transition = def.findEventTransion(currentState, event).orElseThrow(() -> new IllegalStateException("for machines of type " + def.getName() + " event " + event.toString() + " does not trigger any transition out of state " + currentState.toString()));
        final Optional<Consumer<Map<String, String>>> action = transition.getAction();
        try {
            action.ifPresent(mapConsumer -> mapConsumer.accept(context));
            error = Optional.empty();
        } catch (Throwable t) {
            error = Optional.of(t);
            throw new TransitionException(MachineInstanceImpl.this, transition);
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
                throw new TransitionException(MachineInstanceImpl.this, transition);
            }
        }
    }

    private boolean recordEventTransition(E event) {
//        return history.add(new TransitionRecord<>(currentState, event, Instant.now()));
        return true;
    }

    private void recordStpTransition() {
        history.add(new TransitionRecord<>(currentState, Instant.now()));
    }
}
