package com.thekirschners.statusmachina.core;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class Transition<S,E> {
    private final S from;
    private final S to;
    private final Optional<E> event;
    private Optional<Consumer<Map<String,String>>> action;

    public Transition(S from, S to, E event, Consumer<Map<String,String>> action) {
        this.from = from;
        this.to = to;
        this.event = Optional.of(event);
        this.action = Optional.of(action);
    }

    public Transition(S from, S to, E event) {
        this.from = from;
        this.to = to;
        this.event = Optional.of(event);
        this.action = Optional.empty();
    }

    public Transition(S from, S to, Consumer<Map<String,String>> action) {
        this.from = from;
        this.to = to;
        this.event = Optional.empty();
        this.action = Optional.of(action);
    }

    public Transition(S from, S to) {
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

    public Optional<Consumer<Map<String, String>>> getAction() {
        return action;
    }
}