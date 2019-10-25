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
