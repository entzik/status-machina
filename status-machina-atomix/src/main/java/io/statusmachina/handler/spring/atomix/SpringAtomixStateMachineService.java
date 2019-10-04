package io.statusmachina.handler.spring.atomix;

import io.statusmachina.core.TransitionException;
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.Machine;
import io.statusmachina.core.api.MachineBuilder;
import io.statusmachina.core.api.MachineSnapshot;
import io.statusmachina.core.spi.StateMachineService;
import io.atomix.core.map.AtomicMap;
import io.atomix.utils.time.Versioned;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SpringAtomixStateMachineService implements StateMachineService {

    @Autowired
    AtomicMap<String, Machine<?,?>> stateMachinesMap;

    @Autowired
    MachineBuilder machineInstanceBuilder;

    @Override
    public <S, E> Machine<S, E> newMachine(MachineDefinition<S, E> type, Map<String, String> context) throws TransitionException {
        return machineInstanceBuilder.withContext(context).ofType(type).build();
    }

    @Override
    public <S, E> Machine<S, E> read(MachineDefinition<S, E> def, String id) throws TransitionException {
        final Versioned<Machine<?,?>> versioned = stateMachinesMap.get(id);
        return versioned == null ? null : ((Machine<S, E>) versioned.value()).deepClone().setStateVersion(versioned.version());
    }

    @Override
    public <S, E> void create(Machine<S, E> instance) {
        final String id = instance.getId();
        if (stateMachinesMap.get(id) != null)
            throw new IllegalStateException("machine with same ID already exists");
        else {
            stateMachinesMap.put(id, instance);
        }
    }

    @Override
    public <S, E> void update(Machine<S, E> instance) {
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

    @Override
    public List<MachineSnapshot> findTerminated() {
        return null;
    }
}
