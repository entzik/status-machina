package com.thekirschners.statusmachina.core;

import java.time.Instant;
import java.util.Optional;

public class TransitionResult<S, E> {
    private S state;
    private Optional<E> event;
    Instant when;

    public TransitionResult() {
    }

    public TransitionResult(S state, E event, Instant when) {
        this.state = state;
        this.event = Optional.of(event);
        this.when = when;
    }

    public TransitionResult(S state, Instant when) {
        this.state = state;
        this.event = Optional.empty();
        this.when = when;
    }

    public S getState() {
        return state;
    }

    public TransitionResult<S, E> setState(S state) {
        this.state = state;
        return this;
    }

    public Optional<E> getEvent() {
        return event;
    }

    public TransitionResult<S, E> setEvent(E event) {
        this.event = Optional.of(event);
        return this;
    }

    public TransitionResult<S, E> noEvent() {
        this.event = Optional.empty();
        return this;
    }

    public Instant getWhen() {
        return when;
    }

    public TransitionResult<S, E> setWhen(Instant when) {
        this.when = when;
        return this;
    }
}
