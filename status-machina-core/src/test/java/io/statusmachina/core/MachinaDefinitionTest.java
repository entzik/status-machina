/*
 *
 * Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package io.statusmachina.core;

import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.Transition;
import io.statusmachina.core.stdimpl.EnumBasedMachineDefinitionBuilderProvider;
import org.junit.jupiter.api.Test;

import static io.statusmachina.core.api.Transition.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


public class MachinaDefinitionTest {
    final Transition<States, Events> t1 = stp(States.S1, States.S2);
    final Transition<States, Events> t2 = event(States.S2, States.S3, Events.E23);
    final Transition<States, Events> t3 = event(States.S3, States.S4, Events.E34);
    final Transition<States, Events> t4 = event(States.S3, States.S5, Events.E35);

    final MachineDefinition<States, Events> def = new EnumBasedMachineDefinitionBuilderProvider().getMachineDefinitionBuilder(States.class, Events.class)
            .name("toto")
            .states(States.values())
            .initialState(States.S1)
            .idleStates(States.S2, States.S3)
            .terminalStates(States.S4, States.S5)
            .events(Events.values())
            .transitions(t1, t2, t3, t4)
            .build();

    @Test
    void testTransitions() {
        assertThat(t1.isSTP()).isTrue().as("transition t1 correctly identified as STP");
        assertThat(t1.getFrom()).isEqualTo(States.S1).as("transition t1 has source state properly identified");
        assertThat(t1.getTo()).isEqualTo(States.S2).as("transition t1 has source state properly identified");
        assertThat(t2.isSTP()).isFalse().as("transition t2 correctly identified as not STP");
        assertThat(t2.getFrom()).isEqualTo(States.S2).as("transition t2 has source state properly identified");
        assertThat(t2.getTo()).isEqualTo(States.S3).as("transition t2 has source state properly identified");
        assertThat(t2.getEvent().get()).isEqualTo(Events.E23).as("transition t2 has event properly identified");
    }

    @Test
    void testValidDef() {
        assertThat(def).isNotNull().as("a state machine definition was created");
        assertThat(def.getName()).isEqualTo("toto").as("state machine definition bares expected name");
        assertThat(def.getAllStates()).containsOnly(States.values()).as("state machine definition was correctly configured with all states");
        assertThat(def.getInitialState()).isEqualTo(States.S1).as("state machine configured with expected initial state");
        assertThat(def.getTerminalStates()).containsOnly(States.S4, States.S5).as("state machine configured with expected terminal states");
        assertThat(def.getIdleStates()).containsOnly(States.S2, States.S3);
    }

    enum States {
        S1, S2, S3, S4, S5
    }

    enum Events {
        E23, E34, E35
    }
}
