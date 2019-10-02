package com.thekirschners.statusmachina.handler.springjpa;

import com.thekirschners.statusmachina.core.MachineInstanceImpl;
import com.thekirschners.statusmachina.core.TransitionException;
import com.thekirschners.statusmachina.core.api.MachineDef;
import com.thekirschners.statusmachina.core.api.MachineInstance;
import com.thekirschners.statusmachina.core.api.MachineInstanceBuilder;
import com.thekirschners.statusmachina.core.api.MachineSnapshot;
import com.thekirschners.statusmachina.core.spi.StateMachineService;
import com.thekirschners.statusmachina.handler.springjpa.model.ExternalState;
import com.thekirschners.statusmachina.handler.springjpa.repo.ExternalStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SpringJpaStateMachineService implements StateMachineService {
    public static final String ERROR_STATE = "__ERROR_STATE__";
    @Autowired
    ExternalStateRepository externalStateRepository;

    @Autowired
    MachineInstanceBuilder machineInstanceBuilder;

    @Override
    public <S, E> MachineInstance<S, E> newMachine(MachineDef<S, E> def, Map<String, String> context) throws TransitionException {
        return machineInstanceBuilder.ofType(def).withContext(context).build();
    }

    @Override
    public <S, E> MachineInstance<S, E> read(MachineDef<S, E> def, String id) throws TransitionException {
        final ExternalState externalState = externalStateRepository.findById(id).orElseThrow();
        final Map<String, String> context = externalState.getContext();
        final S currentstate = def.getStringToState().apply(externalState.getCurrentState());

        return new MachineInstanceImpl<>(id, def, currentstate, context, Collections.emptyList(), Optional.empty());
    }

    @Override
    public <S, E> void create(MachineInstance<S, E> instance) {
        final ExternalState entity = extractExternalState(instance);
        externalStateRepository.save(entity);
    }

    @Override
    public <S, E> void update(MachineInstance<S, E> instance) {
        final ExternalState currentState = externalStateRepository.findById(instance.getId()).orElseThrow();
        final ExternalState updatedState = updateExternalState(currentState, instance);
        externalStateRepository.save(updatedState);
    }

    @Override
    public List<MachineSnapshot> findStale(long minutes) {
        final long staleReference = Instant.now().toEpochMilli() - Duration.ofMinutes(minutes).toMillis();
        final List<ExternalState> states = externalStateRepository.findAllByLastModifiedEpochLessThan(staleReference);
        return getMachineSnapshots(states);
    }

    @Override
    public List<MachineSnapshot> findFailed() {
        final List<ExternalState> states = externalStateRepository.findAllByCurrentState(ERROR_STATE);
        return getMachineSnapshots(states);
    }

    private List<MachineSnapshot> getMachineSnapshots(List<ExternalState> states) {
        return states
                .stream().map(state -> new MachineSnapshot(state.getType(), state.getId(), state.getCurrentState(), state.getContext(), state.getError()))
                .collect(Collectors.toList());
    }

    private <S, E> ExternalState extractExternalState(MachineInstance<S, E> machineInstance) {
        ExternalState currentState = new ExternalState();

        currentState
                .setId(machineInstance.getId())
                .setType(machineInstance.getDef().getName())
                .setCurrentState(machineInstance.isErrorState() ? ERROR_STATE : machineInstance.getDef().getStateToString().apply(machineInstance.getCurrentState()))
                .setError(machineInstance.getError().orElse("no error"))
                .setContext(new HashMap<>(machineInstance.getContext()))
                .setLocked(true)
                .setLastModifiedEpoch(Instant.now().toEpochMilli());

        return currentState;
    }

    private <S, E> ExternalState updateExternalState(ExternalState currentState, MachineInstance<S, E> machineInstance) {
        currentState.setId(machineInstance.getId());
        currentState.setType(machineInstance.getDef().getName());
        currentState.setCurrentState(machineInstance.getDef().getStateToString().apply(machineInstance.getCurrentState()));

        applyTargetContext(currentState, machineInstance);

        return currentState;

    }

    private <S, E> void applyTargetContext(ExternalState currentState, MachineInstance<S, E> machineInstance) {
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
}
