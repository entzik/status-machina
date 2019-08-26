package com.thekirschners.statusmachina.handler;

import com.thekirschners.statusmachina.core.MachineDef;
import com.thekirschners.statusmachina.core.MachineInstance;
import com.thekirschners.statusmachina.core.TransitionException;

import java.util.Map;

public interface StateMachineLockService {
    void lock(String id);
    void release(String id);
}
