package com.thekirschners.statusmachina.core;

import java.time.Instant;
import java.util.Optional;

public class TransitionRecord<S, E> {
    final private S state;
    final private Optional<E> event;
    final private Instant when;
    final private Optional<String> error;

    public TransitionRecord(S state, E event, Instant when) {
        this.state = state;
        this.event = Optional.of(event);
        this.when = when;
        this.error = Optional.empty();
    }

    public TransitionRecord(S state, E event, String error, Instant when) {
        this.state = state;
        this.event = Optional.of(event);
        this.when = when;
        this.error = Optional.of(error);
    }

    public TransitionRecord(S state, Instant when) {
        this.state = state;
        this.event = Optional.empty();
        this.when = when;
        this.error = Optional.empty();
    }

    public TransitionRecord(S state, String error, Instant when) {
        this.state = state;
        this.event = Optional.empty();
        this.when = when;
        this.error = Optional.of(error);
    }

    public S getState() {
        return state;
    }

    public Optional<E> getEvent() {
        return event;
    }


    public Instant getWhen() {
        return when;
    }


    public Optional<String> getError() {
        return error;
    }
}
