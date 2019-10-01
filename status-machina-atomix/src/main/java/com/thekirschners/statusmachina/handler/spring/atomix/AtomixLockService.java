package com.thekirschners.statusmachina.handler.spring.atomix;

import com.thekirschners.statusmachina.core.spi.StateMachineLockService;
import org.springframework.stereotype.Service;

@Service()
public class AtomixLockService implements StateMachineLockService {
    @Override
    public void lock(String id) {

    }

    @Override
    public void release(String id) {

    }
}
