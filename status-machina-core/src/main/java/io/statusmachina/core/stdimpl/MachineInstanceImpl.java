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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.statusmachina.core.api.*;
import io.statusmachina.core.spi.MachinePersistenceCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

public class MachineInstanceImpl<S, E> implements Machine<S, E> {
    private final Logger LOGGER = LoggerFactory.getLogger(MachineInstanceImpl.class);

    private final String id;

    private final MachineDefinition<S, E> def;
    private final ImmutableMap<String, String> context;
    private final ImmutableList<TransitionRecord<S, E>> history;
    private final ErrorType errorType;
    private final Optional<String> error;

    private final S currentState;
    private final MachinePersistenceCallback<S, E> persistenceCallback;

    public MachineInstanceImpl(
            MachineDefinition<S, E> def,
            MachinePersistenceCallback<S, E> persistenceCallback,
            Map<String, String> context
    ) throws Exception {
        this(def, UUID.randomUUID().toString(), persistenceCallback, context);
    }

    public MachineInstanceImpl(
            MachineDefinition<S, E> def,
            String id,
            MachinePersistenceCallback<S, E> persistenceCallback,
            Map<String, String> context
    ) throws Exception {
        this(def, id, context, persistenceCallback);
    }

    public MachineInstanceImpl(
            MachineDefinition<S, E> def,
            String id,
            Map<String, String> context, MachinePersistenceCallback<S, E> persistenceCallback
    ) throws Exception {
        this.def = def;

        this.id = id;
        this.history = ImmutableList.<TransitionRecord<S, E>>builder().build();
        this.currentState = def.getInitialState();
        this.context = ImmutableMap.<String, String>builder().putAll(context).build();
        this.error = Optional.empty();
        this.errorType = ErrorType.NONE;
        this.persistenceCallback = persistenceCallback;

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("creating a new state machine instance of type {}, with ID {}, in initial state, preparing to persist", def.getName(), id);
        persistenceCallback.runInTransaction(() -> persistenceCallback.saveNew(this));
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("state machine instance of type {}, with ID {} persisted", def.getName(), id);
    }

    public MachineInstanceImpl(
            String id,
            MachineDefinition<S, E> def,
            S currentState,
            Map<String, String> context,
            List<TransitionRecord<S, E>> history,
            ErrorType errorType,
            Optional<String> error,
            MachinePersistenceCallback<S, E> persistenceCallback
    ) throws TransitionException {
        this.def = def;

        this.id = id;
        this.history = ImmutableList.<TransitionRecord<S, E>>builder().addAll(history).build();
        this.error = error;
        this.currentState = currentState;
        this.context = ImmutableMap.<String, String>builder().putAll(context).build();
        this.errorType = errorType;
        this.persistenceCallback = persistenceCallback;
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("creating a new state machine instance of type {}, with ID {} in state {}", def.getName(), id, def.getStateToString().apply(currentState));
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
    public ImmutableMap<String, String> getContext() {
        return ImmutableMap.<String, String>builder().putAll(context).build();
    }

    @Override
    public List<TransitionRecord<S, E>> getHistory() {
        return new ArrayList<>(history);
    }

    @Override
    public ErrorType getErrorType() {
        return errorType;
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

    public Machine<S, E> start() {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("starting state machine instance of type {}, with ID {}", def.getName(), id);
        if (currentState.equals(def.getInitialState()))
            return tryStp();
        else
            throw new IllegalStateException("machine is already started");
    }

    public Machine<S, E> resume() {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("resuming state machine instance of type {}, with ID {}, out of state {}", def.getName(), id, def.getStateToString().apply(currentState));
        if (currentState.equals(def.getInitialState())) {
            throw new IllegalStateException("machine cannot be resumed out of the initial state, call start()");
        } else if (isErrorState()) {
            throw new IllegalStateException("machine cannot be resumed out of error state:  type " + def.getName() + ", id " + id + "  error " + error.get());
        } else
            return tryStp();
    }

    @Override
    public Machine<S, E> sendEvent(E event) throws TransitionException {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("state machine instance of type {}, with ID {} receives event {}", def.getName(), id, def.getEventToString().apply(event));
        if (def.getTerminalStates().contains(currentState)) {
            LOGGER.debug("state machine instance of type {}, with ID {} receives event {} while in terminal state {}. Aborting.", def.getName(), id, def.getEventToString().apply(event), currentState);
            throw new IllegalStateException(new IllegalStateException("state machine of type " + def.getName() + " with ID " + id + " event " + event.toString() + " has received an event while in ternminal state " + currentState + ". Aborting."));
        } else if (isErrorState()) {
            LOGGER.error("a state machine cannot accept event when in error state:  type {}, id {}, error '{}'", def.getName(), id, error.get());
            throw new IllegalStateException("a state machine cannot accept event when in error state:  type " + def.getName() + ", id " + id + "  error " + error.get());
        } else {
            Transition<S, E> transition = def.findEventTransition(currentState, event).orElseThrow(() -> new IllegalStateException("for machines of type " + def.getName() + " event " + event.toString() + " does not trigger any transition out of state " + currentState.toString()));
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("a transition was found for machine instance of type {}, with ID {} from state {} to state {} on event {}",
                        def.getName(),
                        id,
                        def.getEventToString().apply(event),
                        def.getStateToString().apply(currentState),
                        def.getStateToString().apply(transition.getTo())
                );

            return applyTransition(transition, null);
        }
    }

    @Override
    public <P> Machine<S, E> sendEvent(E event, P param) throws TransitionException {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("state machine instance of type {}, with ID {} receives event {} with parameter {}", def.getName(), id, def.getEventToString().apply(event), param.toString());
        if (def.getTerminalStates().contains(currentState)) {
            LOGGER.debug("state machine instance of type {}, with ID {} receives event {} while in terminal state {}. Aborting.", def.getName(), id, def.getEventToString().apply(event), currentState);
            throw new IllegalStateException(new IllegalStateException("state machine of type " + def.getName() + " with ID " + id + " event " + event.toString() + " has received an event while in ternminal state " + currentState + ". Aborting."));
        } else if (isErrorState()) {
            LOGGER.error("a state machine cannot accept event when in error state:  type {}, id {}, error '{}'", def.getName(), id, error.get());
            throw new IllegalStateException("a state machine cannot accept event when in error state:  type " + def.getName() + ", id " + id + "  error " + error.get());
        }
        {
            final Transition<S, E> transition = def.findEventTransition(currentState, event).orElseThrow(() -> new IllegalStateException("for machines of type " + def.getName() + " event " + event.toString() + " does not trigger any transition out of state " + currentState.toString()));
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("a transition was found for machine instance of type {}, with ID {} from state {} to state {} on event {}",
                        def.getName(),
                        id,
                        def.getEventToString().apply(event),
                        def.getStateToString().apply(currentState),
                        def.getStateToString().apply(transition.getTo())
                );
            return applyTransition(transition, param);
        }
    }

    @Override
    public Machine<S, E> recoverFromError(S state, Map<String, String> context) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("state machine instance of type {}, with ID {} recovering from error to state {}, with context", def.getName(), id, def.getStateToString().apply(state));

        Optional<String> newError = Optional.empty();
        ImmutableMap<String, String> newContext = ImmutableMap.<String, String>builder().putAll(context).build();

        return new MachineInstanceImpl<S, E>(id, def, state, newContext, history, ErrorType.NONE, newError, persistenceCallback);
    }

    private Machine<S, E> tryStp() throws TransitionException {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("checking stp transitions for state machine instance of type {}, with ID {}, out of state {}", def.getName(), id, def.getStateToString().apply(currentState));
        if (this.isErrorState())
            return this;
        else {
            return def.findStpTransition(currentState, context).map(t -> {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("stp transitions for state machine instance of type {}, with ID {}, out of state {}, to state {}", def.getName(), id, def.getStateToString().apply(currentState), def.getStateToString().apply(t.getTo()));
                return applyTransition(t, null);
            }).orElse(this);
        }
    }

    private <P> Machine<S, E> applyTransition(Transition<S, E> transition, P param) throws TransitionException {
        S newState = transition.getTo();

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("preparing to apply transition  for state machine instance of type {}, with ID {}, out of state {} to state {}", def.getName(), id, def.getStateToString().apply(currentState), def.getStateToString().apply(newState));
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("starting persistence transaction for state machine instance of type {}, with ID {}", def.getName(), id);
        final MachineAndStash<S, E> machineAndStash;
        try {
            machineAndStash = persistenceCallback.runInTransaction(() -> {
                final Optional<TransitionAction<?>> action = transition.getAction();
                try {
                    ImmutableMap<String, String> newContext = action.map(
                            mapConsumer -> {
                                if (LOGGER.isDebugEnabled())
                                    LOGGER.debug("executing transition  action for state machine instance of type {}, with ID {}, out of state {} to state {}", def.getName(), id, def.getStateToString().apply(currentState), def.getStateToString().apply(newState));
                                return ((TransitionAction<P>) mapConsumer).apply(context, param);
                            }
                    ).orElse(context);
                    Optional<String> newError = Optional.empty();
                    final MachineInstanceImpl<S, E> newMachine = new MachineInstanceImpl<>(id, def, newState, newContext, history, ErrorType.NONE, newError, persistenceCallback);
                    final ImmutableMap<String, Object> stashStore = action.map(TransitionAction::getStashStore).orElseGet(() -> ImmutableMap.<String, Object>builder().build());
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("transition for state machine instance of type {}, with ID {}, out of state {} to state {} completed, preparing to save", def.getName(), id, def.getStateToString().apply(currentState), def.getStateToString().apply(newState));
                    final Machine<S, E> updatedMachine = update(newMachine, transition, param, null);
                    return new MachineAndStash<>(updatedMachine, stashStore);
                } catch (Throwable t) {
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("transition for state machine instance of type {}, with ID {}, out of state {} to state {} failed, switching to error state and saving", def.getName(), id, def.getStateToString().apply(currentState), def.getStateToString().apply(newState));
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("commited persistence transaction for state machine instance of type {}, with ID {} - invoking error handler and saving", def.getName(), id);
                    final Machine<S, E> updatedMachine = applyErrorState(transition, param, t, ErrorType.TRANSITION);
                    return new MachineAndStash<>(updatedMachine, t);
                }
            });
        } catch (Exception e) {
            throw new TransitionException(ErrorType.TRANSITION, e);
        }

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("commited persistence transaction for state machine instance of type {}, with ID {}", def.getName(), id);
        final Machine<S, E> machine = machineAndStash.getMachine();

        if (machine.isErrorState()) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("machine instance of type {}, with ID {} is in error state, skipping post action", def.getName(), id);
            return machine;
            //throw new TransitionException(machineAndStash.getMachine(), transition, ErrorType.TRANSITION, machineAndStash.getErrorCause());
        } else {
            transition.getPostAction().ifPresent(pa -> {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("executing post transition action for state machine instance of type {}, with ID {}", def.getName(), id);
                final TransitionPostAction<P> postAction = (TransitionPostAction<P>) pa;
                try {
                    postAction.setStash(ImmutableMap.<String, Object>builder().putAll(machineAndStash.getStashStore()).build());
                    postAction.accept(machineAndStash.getMachine().getContext(), param);
                } catch (Throwable t) {
                    applyErrorState(transition, param, t, ErrorType.POST_TRANSITION);
                    throw new TransitionException(machine, transition, ErrorType.POST_TRANSITION, t);
                }
            });
            return ((MachineInstanceImpl) machine).tryStp();
        }
    }

    private <P> Machine<S, E> applyErrorState(Transition<S, E> transition, P param, Throwable t, ErrorType newErrorType) {
        final String message = t.getMessage();
        Optional<String> newError = Optional.of(message == null ? t.getClass().getSimpleName() : message);
        final MachineInstanceImpl<S, E> newMachine = new MachineInstanceImpl<>(id, def, currentState, context, history, newErrorType, newError, persistenceCallback);
        return update(newMachine, transition, param, t);
    }

    private <P> Machine<S, E> update(Machine<S, E> newMachine, Transition<S, E> transition, P param, Throwable t) {
        long now = Instant.now().toEpochMilli();
        if (newMachine.getErrorType() == ErrorType.TRANSITION) {
            def.getErrorHandler().accept(new DefaultErrorData<>(now, transition, param, t, false));
        } else if (newMachine.getErrorType() == ErrorType.POST_TRANSITION) {
            def.getErrorHandler().accept(new DefaultErrorData<>(now, transition, param, t, true));
        } else if (newMachine.getErrorType() == ErrorType.NONE && def.findStpTransition(newMachine.getCurrentState(), context).isEmpty()) {
            def.getTransitionHandler().accept(new DefaultTransitionData<>(now, transition, param));
        }
        return persistenceCallback.update(newMachine, now);
    }

    @Override
    public boolean isTerminalState() {
        return def.getTerminalStates().contains(currentState);
    }

    @Override
    public boolean isIdleState() {
        return def.getIdleStates().contains(currentState);
    }

    private class DefaultErrorData<S, E, P> implements ErrorData<S, E> {
        private final Transition<S, E> transition;
        private final P param;
        private final Throwable t;
        private final boolean isPostActionError;
        private final long lastModifiedEpoch;

        public DefaultErrorData(long lastModifiedEpoch, Transition<S, E> transition, P param, Throwable t, boolean isPostActionError) {
            this.lastModifiedEpoch = lastModifiedEpoch;
            this.transition = transition;
            this.param = param;
            this.t = t;
            this.isPostActionError = isPostActionError;
        }

        @Override
        public String getStateMachineId() {
            return getId();
        }

        @Override
        public String getStateMachineType() {
            return def.getName();
        }

        @Override
        public long getLastModifiedEpoch() {
            return lastModifiedEpoch;
        }

        @Override
        public boolean isPostActionError() {
            return isPostActionError;
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

        @Override
        public Throwable getCause() {
            return t;
        }
    }

    private class DefaultTransitionData<S, E, P> implements TransitionData<S, E> {
        private final Transition<S, E> transition;
        private final P param;
        private final long lastModifiedEpoch;

        public DefaultTransitionData(long lastModifiedEpoch, Transition<S, E> transition, P param) {
            this.lastModifiedEpoch = lastModifiedEpoch;
            this.transition = transition;
            this.param = param;
        }

        @Override
        public String getStateMachineId() {
            return getId();
        }

        @Override
        public String getStateMachineType() {
            return def.getName();
        }

        @Override
        public long getLastModifiedEpoch() {
            return lastModifiedEpoch;
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
    }
}
