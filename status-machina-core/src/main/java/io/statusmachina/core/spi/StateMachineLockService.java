package io.statusmachina.core.spi;

public interface StateMachineLockService {
    void lock(String id);
    void release(String id);
}
