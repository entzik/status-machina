package io.statusmachina.handler.spring.atomix.configuration;

import io.atomix.protocols.raft.MultiRaftProtocol;
import io.atomix.protocols.raft.ReadConsistency;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AtomixMultiRaftProtocolConfiguration {
    @Bean
    public MultiRaftProtocol getMultiRaftProtocol() {
        return MultiRaftProtocol.builder("raft")
                .withReadConsistency(ReadConsistency.LINEARIZABLE)
                .build();
    }
}
