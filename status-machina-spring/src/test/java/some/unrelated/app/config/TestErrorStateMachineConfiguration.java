/*
 *  Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */

package some.unrelated.app.config;

import com.google.common.collect.ImmutableMap;
import io.statusmachina.core.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static io.statusmachina.core.api.Transition.event;
import static io.statusmachina.core.api.Transition.stp;

@Configuration
public class TestErrorStateMachineConfiguration {
    public enum States {
        S1, S2, S3, S4
    }

    public enum Events {
        E23, E34, E35
    }


    final Transition<States, Events> t1 = stp(States.S1, States.S2, new TransitionActionBase<String>() {
        @Override
        public ImmutableMap<String, String> apply(ImmutableMap<String, String> context, String parameter) {
            return context;
        }
    });
    final Transition<States, Events> t21 = stp(States.S2, States.S3, new TransitionActionBase<String>() {
        @Override
        public ImmutableMap<String, String> apply(ImmutableMap<String, String> context, String parameter) {
            throw new IllegalStateException("whatever");
        }
    });
    final Transition<States, Events> t22 = stp(States.S2, States.S3, new TransitionActionBase<String>() {
        @Override
        public ImmutableMap<String, String> apply(ImmutableMap<String, String> context, String parameter) {
            return context;
        }
    }, (immutableMap, o) -> {
        throw new IllegalStateException("post whatever");
    });
    final Transition<States, Events> t3 = stp(States.S3, States.S4, new TransitionActionBase<String>() {
        @Override
        public ImmutableMap<String, String> apply(ImmutableMap<String, String> context, String parameter) {
            return context;
        }
    });


    @Autowired
    MachineDefinitionBuilderProvider<States, Events> builderProvider;

    @Bean("Test2StateMachineDef")
    public MachineDefinition<States, Events> getMachineDefinition1() {
        return builderProvider.getMachineDefinitionBuilder(States.class, Events.class)
                .name("toto")
                .states(States.values())
                .initialState(States.S1)
                .terminalStates(States.S4)
                .events(Events.values())
                .transitions(t1, t21, t3)
                .build();
    }

    @Bean("Test3StateMachineDef")
    public MachineDefinition<States, Events> getMachineDefinition2() {
        return builderProvider.getMachineDefinitionBuilder(States.class, Events.class)
                .name("toto")
                .states(States.values())
                .initialState(States.S1)
                .terminalStates(States.S4)
                .events(Events.values())
                .transitions(t1, t22, t3)
                .build();
    }
}
