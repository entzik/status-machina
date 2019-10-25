/*
 *
 *  * Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *
 */

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
