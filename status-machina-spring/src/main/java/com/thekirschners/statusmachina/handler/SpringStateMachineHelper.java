package com.thekirschners.statusmachina.handler;

import com.thekirschners.statusmachina.core.TransitionException;
import com.thekirschners.statusmachina.core.api.MachineDef;
import com.thekirschners.statusmachina.core.api.MachineInstance;
import com.thekirschners.statusmachina.core.spi.StateMachineLockService;
import com.thekirschners.statusmachina.core.spi.StateMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Consumer;

@Service
@Transactional
public class SpringStateMachineHelper {
    @Value("${statusmachina.acquire.retries.max:5}")
    private int maxRetries;

    @Value("${statusmachina.acquire.retries.delay.increment:200}")
    private int retryDelay;

    @Autowired
    StateMachineService service;

    @Autowired
    StateMachineLockService lockService;

    public <S, E> void newStateMachine(MachineDef<S, E> def, Map<String, String> context) throws TransitionException {
        final MachineInstance<S, E> machineInstance = service.newMachine(def, context);
        service.create(machineInstance);
        lockService.release(machineInstance.getId());
    }

    public <S, E> void withNewStateMachine(MachineDef<S, E> def, Map<String, String> context, Consumer<MachineInstance<S, E>> consumer) throws TransitionException {
        final MachineInstance<S, E> machineInstance = service.newMachine(def, context);
        service.create(machineInstance);
        try {
            consumer.accept(machineInstance);
            service.update(machineInstance);
        } catch (TransitionException e) {
            service.update(machineInstance);
        } finally {
            lockService.release(machineInstance.getId());
        }
    }

    public <S,E> void withMachine(String id, MachineDef<S, E> def, Consumer<MachineInstance<S, E>> consumer) throws TransitionException {
        waitForMachine(id);
        final MachineInstance<S, E> machineInstance = service.read(def, id);
        try {
            consumer.accept(machineInstance);
            service.update(machineInstance);
        } catch (TransitionException e) {
            service.update(machineInstance);
        } finally {
            lockService.release(id);
        }
    }

    private void waitForMachine(String id) {
        boolean notLocked = true;
        for (int i = 0; i < maxRetries && notLocked; i ++)
            try {
                lockService.lock(id);
                notLocked = false;
            } catch (Exception e) {
                try {
                    Thread.sleep(retryDelay * (i + 1));
                } catch (InterruptedException ex) { /* just ignore */ }
            }
        if (notLocked)
            lockService.lock(id);
    }

}
