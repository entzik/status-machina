package io.statusmachina.core.api;

import java.util.Map;
import java.util.function.BiFunction;

public interface TransitionAction<P> extends BiFunction<Map<String,String>, P, Map<String,String>> {
}
