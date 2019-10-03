package com.thekirschners.statusmachina.core;

import com.thekirschners.statusmachina.core.api.*;

import java.util.*;

public class MachineInstanceImpl<S, E> implements MachineInstance<S, E> {
    final String id;

    private MachineDef<S, E> def;
    private Map<String, String> context;
    private List<TransitionRecord<S,E>> history;
    private Optional<String> error;

    private transient long version;

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
            Optional<String> error
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
        return new HashMap<>(context);
    }

    @Override
    public List<TransitionRecord<S,E>> getHistory() {
        return new ArrayList<>(history);
    }

    @Override
    public Optional<String> getError() {
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
        applyTransition(transition, null);
        tryStp();
    }

    @Override
    public <P> void sendEvent(E event, P param) throws TransitionException {
        Transition<S,E> transition = def.findEventTransion(currentState, event).orElseThrow(() -> new IllegalStateException("for machines of type " + def.getName() + " event " + event.toString() + " does not trigger any transition out of state " + currentState.toString()));
        applyTransition(transition, param);
        tryStp();
    }

    @Override
    public void recoverFromError(S state, Map<String, String> context) {
        this.error = Optional.empty();
        this.currentState = state;
        this.context = context;
    }

    private void tryStp() throws TransitionException {
        Optional<Transition<S, E>> stpTransition;
        while ((stpTransition = def.findStpTransition(currentState)).isPresent()) {
            final Transition<S, E> transition = stpTransition.get();
            applyTransition(transition, null);
        }
    }

    private <P> void applyTransition(Transition<S, E> transition, P param) throws TransitionException {
        final Optional<TransitionAction<?>> action = transition.getAction();
        try {
            context = action.map(mapConsumer -> ((TransitionAction<P>) mapConsumer).apply(context, param)).orElse(Collections.emptyMap());
            error = Optional.empty();
        } catch (Throwable t) {
            def.getErrorHandler().accept(new DefaultErrorData<>(transition, param, t));
            error = Optional.of(t.getMessage());
            throw new TransitionException(MachineInstanceImpl.this, transition);
        }
        currentState = transition.getTo();
        recordStpTransition();
    }

    private boolean recordEventTransition(E event) {
//        return history.add(new TransitionRecord<>(currentState, event, Instant.now()));
        return true;
    }

    private void recordStpTransition() {
//        history.add(new TransitionRecord<>(currentState, Instant.now()));
    }

    @Override
    public MachineInstance<S, E> deepClone() throws TransitionException {
        return new MachineInstanceImpl<>(id, def, currentState, new HashMap<>(context), Collections.emptyList(), error);
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public boolean isTerminalState() {
        return def.getTerminalStates().contains(currentState);
    }

    @Override
    public MachineInstance<S, E> setStateVersion(long version) {
        this.version = version;
        return this;
    }

    private class DefaultErrorData<S, E, P> implements ErrorData<S, E> {
        private final Transition<S, E> transition;
        private final P param;
        private final Throwable t;

        public DefaultErrorData(Transition<S, E> transition, P param, Throwable t) {
            this.transition = transition;
            this.param = param;
            this.t = t;
        }

        @Override
        public S getFrom() {
            return transition.getFrom();
        }

        @Override
        public S getTo() {
            return transition.getTo();
        }

        @Override
        public Optional<E> getEvent() {
            return transition.getEvent();
        }

        @Override
        public Map<String, String> getContext() {
            return new HashMap<>(context);
        }

        @Override
        public P getParam() {
            return param;
        }

        @Override
        public String getMessage() {
            return t.getMessage();
        }
    }
}
