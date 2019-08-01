package com.thekirschners.statusmachina.handler.springjpa;

import com.thekirschners.statusmachina.core.MachineDef;
import com.thekirschners.statusmachina.core.MachineInstance;
import com.thekirschners.statusmachina.core.TransitionException;
import com.thekirschners.statusmachina.handler.StateMachineService;
import com.thekirschners.statusmachina.handler.springjpa.model.ExternalState;
import com.thekirschners.statusmachina.handler.springjpa.repo.ExternalStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class SpringJpaStateMachineService implements StateMachineService {
    @Autowired
    ExternalStateRepository externalStateRepository;

    @Override
    public <S, E> MachineInstance<S, E> newMachine(MachineDef<S, E> def, Map<String, String> context) throws TransitionException {
        return MachineInstance.ofType(def).withContext(context);
    }

    @Override
    public <S, E> MachineInstance<S, E> read(MachineDef<S, E> def, String id) throws TransitionException {
        final ExternalState externalState = externalStateRepository.findById(id).orElseThrow();
        final Map<String, String> context = externalState.getContext();
        final S currentstate = def.getStringToState().apply(externalState.getCurrentState());

        return new MachineInstance<>(id, def, currentstate, context, Collections.emptyList(), Optional.empty());
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
    public void lock(String id) {
        final ExternalState externalState = externalStateRepository.findById(id).orElseThrow();
        if (externalState.isLocked())
            throw new IllegalStateException("machine is locked by another instance, ID=" + id);
        else {
            externalState.setLocked(true);
            externalStateRepository.save(externalState);
        }
    }

    @Override
    public void release(String id) {
        final ExternalState externalState = externalStateRepository.findById(id).orElseThrow();
        if (!externalState.isLocked())
            throw new IllegalStateException("machine is not locked, ID=" + id);
        externalState.setLocked(false);
        externalStateRepository.save(externalState);
    }

    private <S, E> ExternalState extractExternalState(MachineInstance<S, E> machineInstance) {
        ExternalState currentState = new ExternalState();

        currentState.setId(machineInstance.getId());
        currentState.setType(machineInstance.getDef().getName());
        currentState.setCurrentState(machineInstance.getDef().getStateToString().apply(machineInstance.getCurrentState()));
        currentState.setContext(new HashMap<>(machineInstance.getContext()));

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
