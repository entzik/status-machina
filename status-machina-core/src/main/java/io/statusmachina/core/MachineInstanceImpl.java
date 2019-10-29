/*
 *
 * Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package io.statusmachina.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.statusmachina.core.api.*;

import java.util.*;

public class MachineInstanceImpl<S, E> implements Machine<S, E> {
    private final String id;

    private final MachineDefinition<S, E> def;
    private final ImmutableMap<String, String> context;
    private final ImmutableList<TransitionRecord<S,E>> history;
    private final Optional<String> error;

    private final S currentState;

    public static <S, E> MachineBuilder ofType(MachineDefinition<S, E> definition) {
        return new MachineInstanceBuilderImpl().ofType(definition);
    }

    MachineInstanceImpl(MachineDefinition<S, E> def, Map<String, String> context) throws TransitionException {
        this(def, UUID.randomUUID().toString(), context);
    }

    MachineInstanceImpl(MachineDefinition<S, E> def,String id,  Map<String, String> context) throws TransitionException {
        this.def = def;
        final StateAndContext kickoff = kickoff(def.getInitialState(), ImmutableMap.<String, String>builder().putAll(context).build());

        this.id = id;
        this.history = ImmutableList.<TransitionRecord<S,E>>builder().build();
        this.currentState = kickoff.getState();
        this.context = kickoff.getContext();
        this.error = Optional.empty();
    }

    public MachineInstanceImpl(
            String id,
            MachineDefinition<S, E> def,
            S currentState,
            Map<String, String> context,
            List<TransitionRecord<S, E>> history,
            Optional<String> error
    ) throws TransitionException {
        this.def = def;
        final StateAndContext kickoff = currentState == def.getInitialState() ?
                kickoff(def.getInitialState(), ImmutableMap.<String, String>builder().putAll(context).build())
                : new StateAndContext(currentState, ImmutableMap.<String, String>builder().putAll(context).build());

        this.id = id;
        this.history = ImmutableList.<TransitionRecord<S,E>>builder().addAll(history).build();
        this.error = error;
        this.currentState = kickoff.getState();
        this.context = kickoff.getContext();
    }

    private StateAndContext kickoff(S initialState, ImmutableMap<String,String> initialContext) {
        S runningState = initialState;
        ImmutableMap<String,String> runningContext = ImmutableMap.<String, String>builder().putAll(initialContext).build();
        Optional<Transition<S,E>> transition;
        while ((transition = def.findStpTransition(runningState)).isPresent()) {
            final Transition<S, E> seTransition = transition.get();
            final Optional<TransitionAction<?>> action = seTransition.getAction();
            final ImmutableMap<String, String> tmpContext = ImmutableMap.<String, String>builder().putAll(runningContext).build();
            runningContext = action.map(mapConsumer -> mapConsumer.apply(tmpContext, null)).orElse(tmpContext);
            runningState = seTransition.getTo();
        }

        return new StateAndContext(runningState, runningContext);
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
    public MachineDefinition<S, E> getDefinition() {
        return def;
    }

    @Override
    public boolean isErrorState() {
        return error.isPresent();
    }

    @Override
    public Machine<S,E>  sendEvent(E event) throws TransitionException {
        Transition<S,E> transition = def.findEventTransion(currentState, event).orElseThrow(() -> new IllegalStateException("for machines of type " + def.getName() + " event " + event.toString() + " does not trigger any transition out of state " + currentState.toString()));
        return applyTransition(transition, null);
    }

    @Override
    public <P> Machine<S,E> sendEvent(E event, P param) throws TransitionException {
        final Transition<S,E> transition = def.findEventTransion(currentState, event).orElseThrow(() -> new IllegalStateException("for machines of type " + def.getName() + " event " + event.toString() + " does not trigger any transition out of state " + currentState.toString()));
        return applyTransition(transition, param);
    }

    @Override
    public Machine<S,E> recoverFromError(S state, Map<String, String> context) {
        Optional<String> newError = Optional.empty();
        ImmutableMap<String,String> newContext = ImmutableMap.<String, String>builder().putAll(context).build();

        return new MachineInstanceImpl<S,E>(id, def, state, newContext, history, newError);
    }

    private Machine<S,E> tryStp() throws TransitionException {
        final Optional<Transition<S, E>> stpTransition = def.findStpTransition(currentState);
        if (stpTransition.isPresent()) {
            return applyTransition(stpTransition.get(), null);
        } else
            return this;
    }

    private <P> Machine<S,E> applyTransition(Transition<S, E> transition, P param) throws TransitionException {
        final Optional<TransitionAction<?>> action = transition.getAction();
        try {
            ImmutableMap<String, String> newContext = action.map(mapConsumer -> ((TransitionAction<P>) mapConsumer).apply(context, param)).orElse(context);
            Optional<String> newError = Optional.empty();
            S newState =  transition.getTo();
            return new MachineInstanceImpl<>(id, def, newState, newContext, history, newError).tryStp();
        } catch (Throwable t) {
            def.getErrorHandler().accept(new DefaultErrorData<>(transition, param, t));
            Optional<String> newError = Optional.of(t.getMessage());
            return new MachineInstanceImpl<>(id, def, currentState, context, history, newError);
        }
    }

    @Override
    public boolean isTerminalState() {
        return def.getTerminalStates().contains(currentState);
    }

    private class StateAndContext {
        private final S state;
        private ImmutableMap<String,String> context;

        public StateAndContext(S state, ImmutableMap<String, String> context) {
            this.state = state;
            this.context = context;
        }

        public S getState() {
            return state;
        }

        public ImmutableMap<String, String> getContext() {
            return context;
        }
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
        public P getEventParameter() {
            return param;
        }

        @Override
        public String getErrorMessage() {
            return t.getMessage();
        }
    }
}
