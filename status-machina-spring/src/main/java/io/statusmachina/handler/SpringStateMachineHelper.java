package io.statusmachina.handler;

import io.statusmachina.core.TransitionException;
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.Machine;
import io.statusmachina.core.api.MachineSnapshot;
import io.statusmachina.core.spi.StateMachineLockService;
import io.statusmachina.core.spi.StateMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public <S, E> void newStateMachine(MachineDefinition<S, E> def, Map<String, String> context) throws TransitionException {
        final Machine<S, E> machineInstance = service.newMachine(def, context);
        service.create(machineInstance);
        lockService.release(machineInstance.getId());
    }

    public <S, E> String withNewStateMachine(MachineDefinition<S, E> def, Map<String, String> context, Consumer<Machine<S, E>> consumer) throws TransitionException {
        final Machine<S, E> machineInstance = service.newMachine(def, context);
        service.create(machineInstance);
        try {
            consumer.accept(machineInstance);
            service.update(machineInstance);
            return machineInstance.getId();
        } catch (TransitionException e) {
            service.update(machineInstance);
            return machineInstance.getId();
        } finally {
            lockService.release(machineInstance.getId());
        }
    }

    public <S,E> void withMachine(String id, MachineDefinition<S, E> def, Consumer<Machine<S, E>> consumer) throws TransitionException {
        waitForMachine(id);
        final Machine<S, E> machineInstance = service.read(def, id);
        try {
            consumer.accept(machineInstance);
            service.update(machineInstance);
        } catch (TransitionException e) {
            service.update(machineInstance);
        } finally {
            lockService.release(id);
        }
    }

    public <S,E> Machine<S,E> read(String id, MachineDefinition<S, E> def) {
        return service.read(def, id);
    }

    public List<MachineSnapshot> findStale(long seconds) {
        return service.findStale(seconds);
    }

    public List<MachineSnapshot> findFailed() {
        return service.findFailed();
    }

    public List<MachineSnapshot> findTerminated() {
        return service.findTerminated();
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
