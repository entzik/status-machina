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

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * <p>An action that will be executed after the transition has completed.</p>
 *
 * <p>It is, implemented as a bi-consumer that takes the machine context in the original state and the event parameter.</p>
 *
 * <p>unlike typical java function implementations, implementations of this interface can have side effects</p>
 *
 * @param <P> the type of the event parameter
 */
public interface TransitionPostAction<P> extends BiConsumer<ImmutableMap<String,String>, P> {
    HashMap<String, Object> stashStore = new HashMap<>();

    /**
     * set the stash product by the action configured on the same transition
     * @param stashStore the stash store
     */
    default void setStash(ImmutableMap<String, Object> stashStore) {
        this.stashStore.putAll(stashStore);
    }

    /**
     * return a stashed object
     * @param key the object's key in the stash
     * @param type the type of the stashed object
     * @param <T> the  type of the stashed object
     * @return the stashed object
     */
    default <T> T getStash(String key, Class<T> type) {
        return this.stashStore.containsKey(key) ? type.cast(this.stashStore.get(key)) : null;
    }
}
