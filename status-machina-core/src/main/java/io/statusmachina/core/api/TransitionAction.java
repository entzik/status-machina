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

package io.statusmachina.core.api;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * <p>An action that will be executed as part of the transition.</p>
 *
 * <p>It is, implemented as a bi-function that takes the machine context in the original state and the event parameter and returns a new context.</p>
 *
 * <p>unlike typical java function implementations, implementations of this interface can have side effects</p>
 *
 * @param <P> the type of the event parameter
 */
public interface TransitionAction<P> extends BiFunction<ImmutableMap<String,String>, P, ImmutableMap<String,String>> {
}
