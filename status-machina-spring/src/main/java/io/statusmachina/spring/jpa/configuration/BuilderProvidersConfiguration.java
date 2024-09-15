/*
 *
 *  * Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *
 */

package io.statusmachina.spring.jpa.configuration;

import io.statusmachina.core.api.MachineBuilderProvider;
import io.statusmachina.core.api.MachineDefinitionBuilderProvider;
import io.statusmachina.core.stdimpl.EnumBasedMachineDefinitionBuilderProvider;
import io.statusmachina.core.stdimpl.MachineInstanceBuilderImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BuilderProvidersConfiguration {
    @Bean
    public MachineBuilderProvider getStateMachineBuilder() {
        return MachineInstanceBuilderImpl::new;
    }

    @Bean
    public MachineDefinitionBuilderProvider getMachineDefinitionBuilderProvider() {
        return new EnumBasedMachineDefinitionBuilderProvider();
    }
}
