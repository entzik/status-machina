package com.thekirschners.statusmachina.handler.springjpa;

import com.thekirschners.statusmachina.core.MachineDef;
import com.thekirschners.statusmachina.core.MachineInstance;
import com.thekirschners.statusmachina.core.TransitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Consumer;

@Service
@Transactional
public class SpringStateMachineHelper {
    private static final int MAX_LOCK_RETRIES = 5;

    @Autowired
    SpringJpaStateMachineService service;

    public <S, E> void newStateMachine(MachineDef<S, E> def, Map<String, String> context) throws TransitionException {
        final MachineInstance<S, E> machineInstance = service.newMachine(def, context);
        service.create(machineInstance);
        service.release(machineInstance.getId());
    }

    public <S, E> void withNewStateMachine(MachineDef<S, E> def, Map<String, String> context, Consumer<MachineInstance<S, E>> consumer) throws TransitionException {
        final MachineInstance<S, E> machineInstance = service.newMachine(def, context);
        service.create(machineInstance);
        try {
            consumer.accept(machineInstance);
            service.update(machineInstance);
        } finally {
            service.release(machineInstance.getId());
        }
    }

    public <S,E> void withMachine(String id, MachineDef<S, E> def, Consumer<MachineInstance<S, E>> consumer) throws TransitionException {
        waitForMachine(id);
        final MachineInstance<S, E> machineInstance = service.read(def, id);
        try {
            consumer.accept(machineInstance);
            service.update(machineInstance);
        } finally {
            service.release(id);;
        }
    }

    private void waitForMachine(String id) {
        boolean notLocked = true;
        for (int i = 0; i < MAX_LOCK_RETRIES && notLocked; i ++)
            try {
                service.lock(id);
                notLocked = false;
            } catch (Exception e) {
                try {
                    Thread.sleep(200 * (i + 1));
                } catch (InterruptedException ex) { /* just ignore */ }
            }
        if (notLocked)
            service.lock(id);
    }

}
