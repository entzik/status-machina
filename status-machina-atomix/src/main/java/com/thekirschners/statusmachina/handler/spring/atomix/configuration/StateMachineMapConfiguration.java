package com.thekirschners.statusmachina.handler.spring.atomix.configuration;

import com.thekirschners.statusmachina.core.api.MachineInstance;
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
    public AtomicMap<String, MachineInstance> getStateMachinesMap() {
        return atomix.<String, MachineInstance>atomicMapBuilder("status-machina-instances")
                .withProtocol(protocol)
                .withCacheSize(512)
                .build();
    }
}
