/*
 *
 * Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package io.statusmachina.spring.jpa;

import io.statusmachina.core.stdimpl.MachineInstanceImpl;
import io.statusmachina.core.api.*;
import io.statusmachina.core.spi.MachinePersistenceCallback;
import io.statusmachina.core.spi.StateMachineService;
import io.statusmachina.spring.jpa.model.ExternalState;
import io.statusmachina.spring.jpa.repo.ExternalStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static io.statusmachina.spring.jpa.configuration.StateMachineRetryTemplateConfiguration.RETRY_TEMPLATE_TRANSACTION_RETRY;
import static io.statusmachina.spring.jpa.configuration.TransactionTemplateCnfiguration.STATUS_MACHINA_TRANSACTION_TEMPLATE;

@Service
public class SpringJpaStateMachineService<S, E> implements StateMachineService<S, E>{
    public static final String ERROR_STATE = "__ERROR_STATE__";

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringJpaStateMachineService.class);

    @Autowired
    ExternalStateRepository externalStateRepository;

    @Autowired
    MachineBuilderProvider machineInstanceBuilderProvider;

    @Autowired
    private ApplicationContext context;

    @Autowired
    @Qualifier(STATUS_MACHINA_TRANSACTION_TEMPLATE)
    private TransactionTemplate transactionTemplate;

    @Autowired
    @Qualifier(RETRY_TEMPLATE_TRANSACTION_RETRY)
    private RetryTemplate retryTemplate;

    MachinePersistenceCallback<S, E> machinePersistenceCallback;

    public SpringJpaStateMachineService() {
    }

    @PostConstruct
    public void postConstruct() {
        LOGGER.debug("constructing fine grained persistence callback");
        machinePersistenceCallback = new FineGrainedMachinePersistenceCallback<>(context.getBean(SpringJpaStateMachineService.class), transactionTemplate, retryTemplate);
    }

    @Override
    public Machine<S, E> newMachine(MachineDefinition<S, E> def, Map<String, String> context) throws Exception {
        LOGGER.debug("building a new state machine of type {}", def.getName());
        final Machine machine = machineInstanceBuilderProvider.getMachineBuilder().ofType(def).withContext(context).withPersistence(machinePersistenceCallback).build();
        LOGGER.debug("built a new state machine of type {}, with ID {}", def.getName(), machine.getId());
        return machine;
    }

    @Override
    public Machine<S, E> newMachine(MachineDefinition<S, E> def, String id, Map<String, String> context) throws Exception {
        LOGGER.debug("building a new state machine of type {}, with predefined ID {}", def.getName(), id);
        final Machine machine = machineInstanceBuilderProvider.getMachineBuilder().ofType(def).withContext(context).withPersistence(machinePersistenceCallback).withId(id).build();
        LOGGER.debug("built a new state machine of type {}, with ID {}", def.getName(), machine.getId());
        return machine;
    }

    public ExternalState create(Machine<S, E> instance) {
        final String id = instance.getId();
        final ExternalState entity = externalStateRepository.findById(id).map(es -> updateExternalState(es, instance, Instant.now().toEpochMilli())).orElseGet(() -> extractExternalState(instance));
        return externalStateRepository.save(entity);
    }

    @Override
    public Machine<S, E> read(MachineDefinition<S, E> def, String id) throws Exception {
        final ExternalState externalState = externalStateRepository.findById(id).orElseThrow();
        final Map<String, String> context = externalState.getContext();
        final S currentstate = def.getStringToState().apply(externalState.getCurrentState());
        final ErrorType errorType = externalState.getErrorType();
        final String error = externalState.getError();

        return new MachineInstanceImpl<S, E>(
                id,
                def,
                currentstate,
                context,
                Collections.emptyList(),
                errorType,
                errorType == ErrorType.NONE ? Optional.empty() : Optional.of(error),
                machinePersistenceCallback
        );
    }

    public void update(Machine<S, E> instance, long epochMilliForUpdate) {
        final ExternalState currentState = externalStateRepository.findById(instance.getId()).orElseThrow();
        final ExternalState updatedState = updateExternalState(currentState, instance, epochMilliForUpdate);
        externalStateRepository.save(updatedState);
    }

    @Override
    public List<MachineSnapshot> findStale(long seconds) {
        final long staleReference = Instant.now().toEpochMilli() - Duration.ofSeconds(seconds).toMillis();
        final List<ExternalState> states = externalStateRepository.findAllByLastModifiedEpochLessThan(staleReference);
        return getMachineSnapshots(states);
    }

    @Override
    public List<MachineSnapshot> findFailed() {
        final List<ExternalState> states = externalStateRepository.findAllByCurrentState(ERROR_STATE);
        return getMachineSnapshots(states);
    }

    @Override
    public List<MachineSnapshot> findTerminated() {
        final List<ExternalState> states = externalStateRepository.findAllByDone(true);
        return getMachineSnapshots(states);
    }


    private List<MachineSnapshot> getMachineSnapshots(List<ExternalState> states) {
        return states
                .stream().map(state -> new MachineSnapshot(state.getType(), state.getId(), state.getCurrentState(), state.getContext(), state.getErrorType(), state.getError(), state.getLastModifiedEpoch()))
                .collect(Collectors.toList());
    }

    private <S, E> ExternalState extractExternalState(Machine<S, E> machineInstance) {
        ExternalState currentState = new ExternalState();

        currentState
                .setId(machineInstance.getId())
                .setType(machineInstance.getDefinition().getName())
                .setCurrentState(machineInstance.getDefinition().getStateToString().apply(machineInstance.getCurrentState()))
                .setErrorType(machineInstance.getErrorType())
                .setError(machineInstance.getError().orElse("no error"))
                .setContext(new HashMap<>(machineInstance.getContext()))
                .setLocked(true)
                .setIdle(machineInstance.isIdleState())
                .setDone(machineInstance.isTerminalState())
                .setLastModifiedEpoch(Instant.now().toEpochMilli());

        return currentState;
    }

    private <S, E> ExternalState updateExternalState(ExternalState currentState, Machine<S, E> machineInstance, long epochMilliForUpdate) {
        currentState
                .setType(machineInstance.getDefinition().getName())
                .setCurrentState(machineInstance.getDefinition().getStateToString().apply(machineInstance.getCurrentState()))
                .setErrorType(machineInstance.getErrorType())
                .setError(machineInstance.getError().orElse("no error"))
                .setLocked(true)
                .setIdle(machineInstance.isIdleState())
                .setDone(machineInstance.isTerminalState())
                .setLastModifiedEpoch(epochMilliForUpdate);
        applyTargetContext(currentState, machineInstance);

        return currentState;

    }

    private <S, E> void applyTargetContext(ExternalState currentState, Machine<S, E> machineInstance) {
        final Map<String, String> crtContext = currentState.getContext();
        final Map<String, String> targetContext = machineInstance.getContext();
        for (String targetKey : targetContext.keySet()) {
            final String targetValue = targetContext.get(targetKey);
            if (!crtContext.containsKey(targetKey)) {
                crtContext.put(targetKey, targetValue);
            } else {
                if (!crtContext.get(targetKey).equals(targetValue))
                    crtContext.put(targetKey, targetValue);
            }
        }
        ArrayList<String> keysToRemove = new ArrayList<>();
        for (String currentKey : crtContext.keySet())
            if (!targetContext.containsKey(currentKey))
                keysToRemove.add(currentKey);
        for (String keyToRemove : keysToRemove)
            crtContext.remove(keyToRemove);
    }

    private static class FineGrainedMachinePersistenceCallback<S, E> implements MachinePersistenceCallback<S, E> {
        private SpringJpaStateMachineService stateMachineService;
        private TransactionTemplate transactionTemplate;
        private RetryTemplate transactionRetryTemplate;

        public FineGrainedMachinePersistenceCallback(SpringJpaStateMachineService stateMachineService, TransactionTemplate transactionTemplate, RetryTemplate transactionRetryTemplate) {
            this.stateMachineService = stateMachineService;
            this.transactionTemplate = transactionTemplate;
            this.transactionRetryTemplate = transactionRetryTemplate;
        }

        @Override
        public Machine<S, E> saveNew(Machine<S, E> machine) {
            return transactionRetryTemplate.execute(context -> {
                stateMachineService.create(machine);
                return machine;
            });
        }

        @Override
        public Machine<S, E> update(Machine<S, E> machine, long epochMilliForUpdate) {
            return transactionRetryTemplate.execute(context -> {
                stateMachineService.update(machine, epochMilliForUpdate);
                return machine;
            });
        }

        @Override
        public <R> R runInTransaction(Callable<R> callable) throws Exception {
            return transactionRetryTemplate.execute(context -> transactionTemplate.execute(status -> {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }));
        }
    }
}
