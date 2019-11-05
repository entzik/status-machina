/*
 *
 *  * Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *
 */

package io.statusmachina.spring.jpa;

import io.statusmachina.core.TransitionException;
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.Machine;
import io.statusmachina.core.api.MachineSnapshot;
import io.statusmachina.core.spi.StateMachineLockService;
import io.statusmachina.core.spi.StateMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@Transactional
public class SpringStateMachineHelper {
    @Value("${statusmachina.acquireRetriesMax:5}")
    private int maxRetries;

    @Value("${statusmachina.acquireRetriesDelayIncrement:200}")
    private int retryDelay;

    @Autowired
    StateMachineService service;

    @Autowired
    StateMachineLockService lockService;

    @Autowired
    @Qualifier("sm-machine-acquisition-state-machine")
    RetryTemplate lockRetryTemplate;

    public <S, E> String newStateMachine(MachineDefinition<S, E> def, String id, Map<String, String> context) throws TransitionException {
        final Machine<S, E> machineInstance = service.newMachine(def, id, context);
        try {
            service.create(machineInstance);
            return machineInstance.getId();
        } finally {
            lockService.release(machineInstance.getId());
        }
    }

    public <S, E> String newStateMachine(MachineDefinition<S, E> def, Map<String, String> context) throws TransitionException {
        final Machine<S, E> machineInstance = service.newMachine(def, context);
        try {
            service.create(machineInstance);
            return machineInstance.getId();
        } finally {
            lockService.release(machineInstance.getId());
        }
    }

    public <S, E> String withNewStateMachine(MachineDefinition<S, E> def, String id, Map<String, String> context, Function<Machine<S, E>, Machine<S, E>> function) throws TransitionException {
        final Machine<S, E> machineInstance = service.newMachine(def, id, context);
        service.create(machineInstance);
        try {
            final Machine<S, E> updated = function.apply(machineInstance);
            service.update(updated);
            return updated.getId();
        } catch (TransitionException e) {
            service.update(machineInstance);
            return machineInstance.getId();
        } finally {
            lockService.release(machineInstance.getId());
        }
    }

    public <S, E> String withNewStateMachine(MachineDefinition<S, E> def, Map<String, String> context, Function<Machine<S, E>, Machine<S, E>> function) throws TransitionException {
        final Machine<S, E> machineInstance = service.newMachine(def, context);
        service.create(machineInstance);
        try {
            final Machine<S, E> updated = function.apply(machineInstance);
            service.update(updated);
            return updated.getId();
        } catch (TransitionException e) {
            service.update(machineInstance);
            return machineInstance.getId();
        } finally {
            lockService.release(machineInstance.getId());
        }
    }

    public <S, E> void withMachine(String id, MachineDefinition<S, E> def, Function<Machine<S, E>, Machine<S, E>> function) throws TransitionException {
        waitForMachine(id);
        final Machine<S, E> machineInstance = service.read(def, id);
        try {
            final Machine<S, E> updated = function.apply(machineInstance);
            service.update(updated);
        } catch (TransitionException e) {
            service.update(machineInstance);
        } finally {
            lockService.release(id);
        }
    }

    public <S, E> void sendEventToMachine(String id, MachineDefinition<S, E> def, E event) {
        withMachine(id, def, seMachine -> seMachine.sendEvent(event));
    }

    public <S, E, P> void sendEventToMachine(String id, MachineDefinition<S, E> def, E event, P param) {
        withMachine(id, def, seMachine -> seMachine.sendEvent(event, param));
    }

    public <S, E> Machine<S, E> read(String id, MachineDefinition<S, E> def) {
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
        lockRetryTemplate.execute((RetryCallback<Boolean, IllegalStateException>) context -> lockService.lock(id));
    }
}
