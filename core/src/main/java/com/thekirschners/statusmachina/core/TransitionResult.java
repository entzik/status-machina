package com.thekirschners.statusmachina.core;

import java.time.Instant;
import java.util.Optional;

public class TransitionResult<S, E> {
    private S state;
    private Optional<E> event;
    private Instant when;
    private Optional<String> error;

    public TransitionResult() {
    }

    public TransitionResult(S state, E event, Instant when) {
        this.state = state;
        this.event = Optional.of(event);
        this.when = when;
        this.error = Optional.empty();
    }

    public TransitionResult(S state, E event, String error, Instant when) {
        this.state = state;
        this.event = Optional.of(event);
        this.when = when;
        this.error = Optional.of(error);
    }

    public TransitionResult(S state, Instant when) {
        this.state = state;
        this.event = Optional.empty();
        this.when = when;
        this.error = Optional.empty();
    }

    public TransitionResult(S state, String error, Instant when) {
        this.state = state;
        this.event = Optional.empty();
        this.when = when;
        this.error = Optional.of(error);
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

    public Optional<String> getError() {
        return error;
    }

    public TransitionResult<S, E> setError(String error) {
        this.error = Optional.of(error);
        return this;
    }

    public TransitionResult<S, E> noError() {
        this.error = Optional.empty();
        return this;
    }
}
