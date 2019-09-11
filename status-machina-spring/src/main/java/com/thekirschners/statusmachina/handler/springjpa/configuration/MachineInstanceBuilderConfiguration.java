package com.thekirschners.statusmachina.handler.springjpa.configuration;

import com.thekirschners.statusmachina.core.MachineInstanceBuilderImpl;
import com.thekirschners.statusmachina.core.api.MachineInstanceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MachineInstanceBuilderConfiguration {
    @Bean
    public MachineInstanceBuilder getStateMachineBuilder() {
        return new MachineInstanceBuilderImpl();
    }
}
