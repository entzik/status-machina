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

/**
 * exception thrown when a state machine exeption fails
 */
public class TransitionException extends RuntimeException {
    private final Machine machineInstance;
    private final Transition<?, ?> transition;

    /**
     * @param machineInstance the affected state machine
     * @param transition the transition that fails
     * @param <S> the state type
     * @param <E> the event type
     */
    public <S,E> TransitionException(Machine<S, E> machineInstance, Transition<S, E> transition) {
        this.machineInstance = machineInstance;
        this.transition = transition;
    }

    /**
     * @param machineInstance the affected state machine
     * @param transition the transition that fails
     * @param cause the downstream exception that caused the transition to fail
     * @param <S> the state type
     * @param <E> the event type
     */
    public <S,E> TransitionException(Machine<S, E> machineInstance, Transition<S, E> transition, Throwable cause) {
        super(cause);
        this.machineInstance = machineInstance;
        this.transition = transition;
    }

    /**
     * @return the affected machine
     */
    public Machine getMachineInstance() {
        return machineInstance;
    }

    /**
     * @return transition that failed
     */
    public Transition<?, ?> getTransition() {
        return transition;
    }
}
