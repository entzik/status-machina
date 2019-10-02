package com.thekirschners.statusmachina.handler.spring.atomix;

import com.thekirschners.statusmachina.core.TransitionException;
import com.thekirschners.statusmachina.core.api.MachineDef;
import com.thekirschners.statusmachina.core.api.MachineInstance;
import com.thekirschners.statusmachina.core.api.MachineInstanceBuilder;
import com.thekirschners.statusmachina.core.api.MachineSnapshot;
import com.thekirschners.statusmachina.core.spi.StateMachineService;
import io.atomix.core.map.AtomicMap;
import io.atomix.utils.time.Versioned;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SpringAtomixStateMachineService implements StateMachineService {

    @Autowired
    AtomicMap<String, MachineInstance<?,?>> stateMachinesMap;

    @Autowired
    MachineInstanceBuilder machineInstanceBuilder;

    @Override
    public <S, E> MachineInstance<S, E> newMachine(MachineDef<S, E> type, Map<String, String> context) throws TransitionException {
        return machineInstanceBuilder.withContext(context).ofType(type).build();
    }

    @Override
    public <S, E> MachineInstance<S, E> read(MachineDef<S, E> def, String id) throws TransitionException {
        final Versioned<MachineInstance<?,?>> versioned = stateMachinesMap.get(id);
        return versioned == null ? null : ((MachineInstance<S, E>) versioned.value()).deepClone().setStateVersion(versioned.version());
    }

    @Override
    public <S, E> void create(MachineInstance<S, E> instance) {
        final String id = instance.getId();
        if (stateMachinesMap.get(id) != null)
            throw new IllegalStateException("machine with same ID already exists");
        else {
            stateMachinesMap.put(id, instance);
        }
    }

    @Override
    public <S, E> void update(MachineInstance<S, E> instance) {
        stateMachinesMap.replace(instance.getId(), instance.getVersion(), instance);
    }

    @Override
    public List<MachineSnapshot> findStale(long minutes) {
        return null;
    }

    @Override
    public List<MachineSnapshot> findFailed() {
        return null;
    }
}
