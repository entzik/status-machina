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

import java.util.HashMap;
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
    HashMap<String, Object> stashStore = new HashMap<>();

    @Override
    ImmutableMap<String, String> apply(ImmutableMap<String, String> context, P parameter);

    /**
     * store an object in to a stash for it to be passed along to the {@link TransitionPostAction}
     * @param key the object's key in the stash
     * @param s the object
     * @param <S> the tpe of the object to be stored in the stash
     */
    default <S> void stash(String key, S s) {
        stashStore.put(key, s);
    }

    /**
     * @return the stash store
     */
    default ImmutableMap<String, Object> getStashStore() {
        return ImmutableMap.<String, Object>builder().putAll(stashStore).build();
    }
}
