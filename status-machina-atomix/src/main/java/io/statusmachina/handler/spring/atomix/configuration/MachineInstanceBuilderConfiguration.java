package io.statusmachina.spring.spring.atomix.configuration;

import io.statusmachina.core.MachineInstanceBuilderImpl;
import io.statusmachina.core.api.MachineBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MachineInstanceBuilderConfiguration {
    @Bean
    public MachineBuilder getStateMachineBuilder() {
        return new MachineInstanceBuilderImpl();
    }
}
