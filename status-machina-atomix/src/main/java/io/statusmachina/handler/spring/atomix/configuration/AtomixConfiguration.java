package io.statusmachina.spring.spring.atomix.configuration;

import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AtomixConfiguration {
    @Bean
    public Atomix getAtomixBuilder() {
        final Atomix atomix = Atomix.builder()
                .withMemberId("springClient")
                .withAddress("127.0.0.1:8000")
                .withMembershipProvider(BootstrapDiscoveryProvider.builder()
                        .withNodes(
                                Node.builder()
                                        .withId("atomix-1")
                                        .withAddress("127.0.0.1:5679")
                                        .build()
                        )
                        .build())
                .build();
        atomix.start().join();
        return atomix;
    }
}
