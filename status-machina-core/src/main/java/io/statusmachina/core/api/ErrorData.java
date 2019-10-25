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

package io.statusmachina.core.api;

import java.util.Map;
import java.util.Optional;

/**
 * data structure that describes the context in which an error occurred
 * @param <S> the type of the state machine's state
 * @param <E> the type of event the state machine accepts
 */
public interface ErrorData<S, E> {
    /**
     * @return the state from which the machine was transitioning out
     */
    S getFrom();

    /**
     * @return the state to which the machine was transitioning to
     */
    S getTo();

    /**
     * @return the event that triggered the transition, empty optional if STP
     */
    Optional<E> getEvent();

    /**
     * @return the context as it was in the source state
     */
    Map<String, String> getContext();

    /**
     * @param <P> the type of the parameter
     * @return the parameter passed along with the event
     */
    <P> P getEventParameter();

    /**
     * @return the error message
     */
    String getErrorMessage();
}
