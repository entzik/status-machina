package io.statusmachina.core;

import io.statusmachina.core.api.TransitionAction;

import java.util.Optional;

public class Transition<S,E> {
    private final S from;
    private final S to;
    private final Optional<E> event;
    private Optional<TransitionAction<?>> action;

    public static <S,E> Transition<S,E> event(S from, S to, E event, TransitionAction<?> action) {
        return new Transition<>(from, to, event, action);
    }

    public static <S,E> Transition<S,E> event(S from, S to, E event) {
        return new Transition<>(from, to, event);
    }

    public static <S,E> Transition<S,E> stp(S from, S to, TransitionAction<?> action) {
        return new Transition<>(from, to, action);
    }

    public static <S,E> Transition<S,E> stp(S from, S to) {
        return new Transition<>(from, to);
    }

    private Transition(S from, S to, E event, TransitionAction<?> action) {
        this.from = from;
        this.to = to;
        this.event = Optional.of(event);
        this.action = Optional.of(action);
    }

    private Transition(S from, S to, E event) {
        this.from = from;
        this.to = to;
        this.event = Optional.of(event);
        this.action = Optional.empty();
    }

    private Transition(S from, S to, TransitionAction<?> action) {
        this.from = from;
        this.to = to;
        this.event = Optional.empty();
        this.action = Optional.of(action);
    }

    private Transition(S from, S to) {
        this.from = from;
        this.to = to;
        this.event = Optional.empty();
        this.action = Optional.empty();
    }

    public S getFrom() {
        return from;
    }

    public S getTo() {
        return to;
    }

    public Optional<E> getEvent() {
        return event;
    }

    public boolean isSTP() {
        return !event.isPresent();
    }

    public  Optional<TransitionAction<?>> getAction() {
        return action;
    }
}
