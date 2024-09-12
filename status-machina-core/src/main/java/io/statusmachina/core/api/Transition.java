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

package io.statusmachina.core.api;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Specifies if and how a state machine can transition from one state to another. It matches an edge in the
 * state machine diagram graph.
 * <p>
 * An action can be specified to be executed as part of the transition. If the action throws an exception, the transition
 * will not be cancelled and the machine will be put in an error state.
 * <p>
 * The transition action can interact with external systems and can mutate the machine's context.
 * <p>
 * transitions can be automatic ( STP: Straight Through Processing ) or event triggered
 *
 * @param <S> the state type
 * @param <E> the event type
 */
public class Transition<S, E> {
    public static final Supplier<Long> EVENT_CARDINALITY_OF_ONE_SUPPLIER = () -> 1L;
    public static final Optional<TransitionPostAction<?>> NO_POST_ACTION = Optional.empty();

    public static final Optional<TransitionAction<?>> NO_ACTION = Optional.empty();
    public static final Optional<TransitionGuard> NO_GUARD = Optional.empty();
    private final S from;
    private final S to;
    private final Optional<E> event;
    private final Optional<TransitionGuard> guard;
    private final Optional<TransitionAction<?>> action;
    private final Optional<TransitionPostAction<?>> postAction;
    private final Supplier<Long> eventCardinalitySupplier;

    /**
     * Configure a transition so that if the machine is in a specified current state ("from") and receives the specified
     * event ("event"), it will move to the specified target state ("to") and execute the specified action in the process.
     * <p>
     * The post action is executed after the transition has completed. if a specific service provider executes the
     * transition in a transaction, then the post action must be executed after the the transaction has completed and
     * only if it has completed successfully.
     * <p>
     * Unlike the a transition action, the post transition action is not allowed to mutate the state machine's context.
     *
     * @param from       the current state
     * @param to         the target state
     * @param event      the event that triggers the transition
     * @param action     the action to be executed during the transition
     * @param postAction the action to be executed after the transition has completed
     * @param <S>        the state type
     * @param <E>        the event type
     * @return a {@link Transition} instance
     */
    public static <S, E> Transition<S, E> event(S from, S to, E event, TransitionAction<?> action, TransitionPostAction<?> postAction) {
        return new Transition<>(from, to, event, action, postAction);
    }

    /**
     * Configure a transition so that if the machine is in a specified current state ("from") and receives the specified
     * event ("event"), it will move to the specified target state ("to") and execute the specified action in the process.
     *
     * @param from   the current state
     * @param to     the target state
     * @param event  the event that triggers the transition
     * @param action the action to be executed
     * @param <S>    the state type
     * @param <E>    the event type
     * @return a {@link Transition} instance
     */
    public static <S, E> Transition<S, E> event(S from, S to, E event, TransitionAction<?> action) {
        return new Transition<>(from, to, event, action);
    }

    /**
     * Configure a transition so that if the machine is in a specified current state ("from") and receives the specified
     * event ("event"), it will move to the specified target state ("to").
     *
     * @param from  the current state
     * @param to    the target state
     * @param event the event that triggers the transition
     * @param <S>   the state type
     * @param <E>   the event type
     * @return a {@link Transition} instance
     */
    public static <S, E> Transition<S, E> event(S from, S to, E event) {
        return new Transition<>(from, to, event);
    }

    /**
     * Configure a transition so that if the machine is in a specified current state ("from"), it will move to the
     * specified target state ("to") after receiving the number of events ("event") as returned by the "eventCardinalitySupplier".
     * The specified post action will be executed after the transition has completed.
     *
     * @param from  the current state
     * @param to    the target state
     * @param event the event that triggers the transition
     * @param <S>   the state type
     * @param <E>   the event type
     * @return a {@link Transition} instance
     */
    public static <S, E> Transition<S, E> event(S from, S to, E event, TransitionAction<?> action, Supplier<Long> eventCardinalitySupplier) {
        return new Transition<>(from, to, event, action, eventCardinalitySupplier);
    }

    /**
     * Configure a transition so that if the machine is in a specified current state ("from"), it will move to the
     * specified target state ("to") after receiving the number of events ("event") as returned by the "eventCardinalitySupplier".
     * The specified post action will be executed after the transition has completed.
     *
     * @param from  the current state
     * @param to    the target state
     * @param event the event that triggers the transition
     * @param <S>   the state type
     * @param <E>   the event type
     * @return a {@link Transition} instance
     */
    public static <S, E> Transition<S, E> event(S from, S to, E event, TransitionAction<?> action, TransitionPostAction<?> postAction, Supplier<Long> eventCardinalitySupplier) {
        return new Transition<>(from, to, event, action, postAction, eventCardinalitySupplier);
    }

    /**
     * Configure a transition so that if the machine is in a specified current state ("from") it will automatically
     * move to the specified target state ("to") and execute the specified action in the process.
     * <p>
     * The post action is executed after the transition has completed. If a specific service provider executes the
     * transition in a transaction, then the post action must be executed after the the transaction has completed and
     * only if it has completed successfully.
     * <p>
     * Unlike the a transition action, the post transition action is not allowed to mutate the state machine's context
     *
     * @param from   the current state
     * @param to     the target state
     * @param action the action to be executed
     * @param transitionGuard the guard that will decide if the transition can be executed or not
     * @param <S>    the state type
     * @param <E>    the event type
     * @return a {@link Transition} instance
     */
    public static <S, E> Transition<S, E> stp(S from, S to, TransitionAction<?> action, TransitionPostAction<?> postAction, TransitionGuard transitionGuard) {
        return new Transition<>(from, to, action, postAction, transitionGuard);
    }

    /**
     * Configure a transition so that if the machine is in a specified current state ("from") it will automatically
     * move to the specified target state ("to") and execute the specified action in the process.
     * <p>
     * The post action is executed after the transition has completed. If a specific service provider executes the
     * transition in a transaction, then the post action must be executed after the the transaction has completed and
     * only if it has completed successfully.
     * <p>
     * Unlike the a transition action, the post transition action is not allowed to mutate the state machine's context
     *
     * @param from   the current state
     * @param to     the target state
     * @param action the action to be executed
     * @param <S>    the state type
     * @param <E>    the event type
     * @return a {@link Transition} instance
     */
    public static <S, E> Transition<S, E> stp(S from, S to, TransitionAction<?> action, TransitionPostAction<?> postAction) {
        return new Transition<>(from, to, action, postAction);
    }

    /**
     * Configure a transition so that if the machine is in a specified current state ("from") it will automatically
     * move to the specified target state ("to") and execute the specified action in the process.
     *
     * @param from   the current state
     * @param to     the target state
     * @param action the action to be executed
     * @param transitionGuard the guard that will decide if the transition can be executed or not
     * @param <S>    the state type
     * @param <E>    the event type
     * @return a {@link Transition} instance
     */
    public static <S, E> Transition<S, E> stp(S from, S to, TransitionAction<?> action, TransitionGuard transitionGuard) {
        return new Transition<>(from, to, action, transitionGuard);
    }

    /**
     * Configure a transition so that if the machine is in a specified current state ("from") it will automatically
     * move to the specified target state ("to") and execute the specified action in the process.
     *
     * @param from   the current state
     * @param to     the target state
     * @param action the action to be executed
     * @param <S>    the state type
     * @param <E>    the event type
     * @return a {@link Transition} instance
     */
    public static <S, E> Transition<S, E> stp(S from, S to, TransitionAction<?> action) {
        return new Transition<>(from, to, action);
    }

    /**
     * Configure a transition so that if the machine is in a specified current state ("from") it will automatically
     * move to the specified target state ("to").
     *
     * @param from the current state
     * @param to   the target state
     * @param transitionGuard the guard that will decide if the transition can be executed or not
     * @param <S>  the state type
     * @param <E>  the event type
     * @return a {@link Transition} instance
     */
    public static <S, E> Transition<S, E> stp(S from, S to, TransitionGuard transitionGuard) {
        return new Transition<>(from, to, transitionGuard);
    }

    /**
     * Configure a transition so that if the machine is in a specified current state ("from") it will automatically
     * move to the specified target state ("to").
     *
     * @param from the current state
     * @param to   the target state
     * @param <S>  the state type
     * @param <E>  the event type
     * @return a {@link Transition} instance
     */
    public static <S, E> Transition<S, E> stp(S from, S to) {
        return new Transition<>(from, to);
    }

    private Transition(S from, S to, E event, TransitionAction<?> action, TransitionPostAction<?> postAction) {
        this(from, to, event, action, postAction, () -> 1L);
    }

    private Transition(S from, S to, E event, TransitionAction<?> action, TransitionPostAction<?> postAction, Supplier<Long> eventCardinalitySupplier) {
        this(from, to, Optional.of(event), NO_GUARD, Optional.of(action), Optional.of(postAction), eventCardinalitySupplier);
    }

    private Transition(S from, S to, E event, TransitionAction<?> action, Supplier<Long> eventCardinalitySupplier) {
        this(from, to, Optional.of(event), NO_GUARD, Optional.of(action), NO_POST_ACTION, eventCardinalitySupplier);
    }

    private Transition(S from, S to, E event, TransitionAction<?> action) {
        this(from, to, Optional.of(event), NO_GUARD, Optional.of(action), NO_POST_ACTION, EVENT_CARDINALITY_OF_ONE_SUPPLIER);
    }

    private Transition(S from, S to, E event) {
        this(from, to, Optional.of(event), NO_GUARD, NO_ACTION, NO_POST_ACTION, EVENT_CARDINALITY_OF_ONE_SUPPLIER);
    }

    private Transition(S from, S to, TransitionAction<?> action, TransitionPostAction<?> postAction) {
        this(from, to, Optional.empty(), NO_GUARD, Optional.of(action), Optional.of(postAction), EVENT_CARDINALITY_OF_ONE_SUPPLIER);
    }

    private Transition(S from, S to, TransitionAction<?> action, TransitionPostAction<?> postAction, TransitionGuard transitionGuard) {
        this(from, to, Optional.empty(), Optional.of(transitionGuard), Optional.of(action), Optional.of(postAction), EVENT_CARDINALITY_OF_ONE_SUPPLIER);
    }

    private Transition(S from, S to, TransitionAction<?> action) {
        this(from, to, Optional.empty(), NO_GUARD, Optional.of(action), NO_POST_ACTION, EVENT_CARDINALITY_OF_ONE_SUPPLIER);
    }

    private Transition(S from, S to, TransitionAction<?> action, TransitionGuard transitionGuard) {
        this(from, to, Optional.empty(), Optional.of(transitionGuard), Optional.of(action), NO_POST_ACTION, EVENT_CARDINALITY_OF_ONE_SUPPLIER);
    }

    private Transition(S from, S to) {
        this(from, to, Optional.empty(), NO_GUARD, NO_ACTION, NO_POST_ACTION, EVENT_CARDINALITY_OF_ONE_SUPPLIER);
    }

    private Transition(S from, S to, TransitionGuard transitionGuard) {
        this(from, to, Optional.empty(), Optional.of(transitionGuard), NO_ACTION, NO_POST_ACTION, EVENT_CARDINALITY_OF_ONE_SUPPLIER);
    }

    public Transition(S from, S to, Optional<E> event, Optional<TransitionGuard> guard, Optional<TransitionAction<?>> action, Optional<TransitionPostAction<?>> postAction, Supplier<Long> eventCardinalitySupplier) {
        this.from = from;
        this.to = to;
        this.event = event;
        this.guard = guard;
        this.action = action;
        this.postAction = postAction;
        this.eventCardinalitySupplier = eventCardinalitySupplier;
    }

    /**
     * return the state in which the state machine needs to be in order for this transition to activate (if all other
     * conditions are fulfilled)
     *
     * @return the state
     */
    public S getFrom() {
        return from;
    }

    /**
     * return the state in which this transition will end up should this transition activate and cbe carried over successfully
     *
     * @return the target state
     */
    public S getTo() {
        return to;
    }

    /**
     * returns the event that needs to be sent to the state machine in order to trigger this transition (should all other
     * conditions be fulfilled)
     *
     * @return the event
     */
    public Optional<E> getEvent() {
        return event;
    }

    /**
     * @return true if this is an STP action, false otherwise
     */
    public boolean isSTP() {
        return !event.isPresent();
    }

    /**
     * @return the action to be executed as part of the transition, should one be configured
     */
    public Optional<TransitionAction<?>> getAction() {
        return action;
    }

    /**
     * @return an action to be executed after a the transition has completed and all state and context changes have been
     * commited to underlying storage, along with change of any transactional service the action may have invoked.
     */
    public Optional<TransitionPostAction<?>> getPostAction() {
        return postAction;
    }

    public Optional<TransitionGuard> getGuard() {
        return guard;
    }

    public long getEventCardinality() {
        return eventCardinalitySupplier.get();
    }
}
