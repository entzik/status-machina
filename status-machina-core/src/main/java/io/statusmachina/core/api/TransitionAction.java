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
