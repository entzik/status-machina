package io.statusmachina.handler.spring.atomix.configuration;

import io.statusmachina.core.api.Machine;
import io.atomix.core.Atomix;
import io.atomix.core.map.AtomicMap;
import io.atomix.protocols.raft.MultiRaftProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StateMachineMapConfiguration {
    @Autowired
    Atomix atomix;

    @Autowired
    MultiRaftProtocol protocol;

    @Bean(name = "state-machines-registry")
    public AtomicMap<String, Machine> getStateMachinesMap() {
        return atomix.<String, Machine>atomicMapBuilder("status-machina-instances")
                .withProtocol(protocol)
                .withCacheSize(512)
                .build();
    }
}
