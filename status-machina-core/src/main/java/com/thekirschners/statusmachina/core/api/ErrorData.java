package com.thekirschners.statusmachina.core.api;

import java.util.Map;
import java.util.Optional;

public interface ErrorData<S, E> {
    S getFrom();
    S getTo();
    Optional<E> getEvent();
    Map<String, String> getContext();
    <P> P getParam();
    String getMessage();
}
