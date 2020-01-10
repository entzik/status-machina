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

package some.unrelated.app.tests;

import io.statusmachina.core.spi.StateMachineService;
import io.statusmachina.spring.jpa.SpringJpaStateMachineService;
import some.unrelated.app.TestSpringBootApp;
import io.statusmachina.core.MachineDefImpl;
import io.statusmachina.core.Transition;
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.Machine;
import io.statusmachina.core.api.MachineSnapshot;
import some.unrelated.app.tests.SpringJpaStateMachineServiceTest.Events;
import some.unrelated.app.tests.SpringJpaStateMachineServiceTest.SpyAction;
import some.unrelated.app.tests.SpringJpaStateMachineServiceTest.States;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.statusmachina.core.Transition.event;
import static io.statusmachina.core.Transition.stp;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = TestSpringBootApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class SpringStateMachineHelperTest {
    final SpyAction a1 = new SpyAction();
    final SpyAction a2 = new SpyAction();
    final SpyAction a3 = new SpyAction();
    final SpyAction a4 = new SpyAction();

    final Transition<States, Events> t1 = stp(States.S1, States.S2, a1);
    final Transition<States, Events> t2 = event(States.S2, States.S3, Events.E23, a2);
    final Transition<States, Events> t3 = event(States.S3, States.S4, Events.E34, a3);
    final Transition<States, Events> t4 = event(States.S3, States.S5, Events.E35, a4);

    final MachineDefinition<States, Events> def = MachineDefImpl.<States, Events>newBuilder()
            .setName("toto")
            .states(States.values())
            .initialState(States.S1)
            .terminalStates(States.S4, States.S5)
            .events(Events.values())
            .transitions(t1, t2, t3, t4)
            .eventToString(Enum::name)
            .stringToEvent(Events::valueOf)
            .stateToString(Enum::name)
            .stringToState(States::valueOf)
            .build();

    @Autowired
    StateMachineService service;

    @Test
    public void testStateMachineHelper_newMachineWithId() {
        final String fixedId = UUID.randomUUID().toString();
        System.out.println("fixedId = " + fixedId);
        try {
            service.newMachine(def, fixedId, Map.of()).start();
            final Machine<States, Events> instance = service.read( def, fixedId);
            assertThat(instance.getCurrentState()).isEqualTo(States.S2).as("states match");
        } catch (Exception e) {
            fail("", e);
        }
    }

    @Test
    public void testStateMachineHelper_newMachine() {
        try {
            final String id = service.newMachine(def, new HashMap<>()).start().getId();
            final Machine<States, Events> instance = service.read(def, id);
            assertThat(instance.getCurrentState()).isEqualTo(States.S2).as("states match");
        } catch (Exception e) {
            fail("", e);
        }
    }

    @Test
    public void testStateMachineHelper_processNewWithId() {
        final String fixedId = UUID.randomUUID().toString();
        try {
            final Machine machine = service.newMachine(def, fixedId, new HashMap<>()).start();
            final String machineId = machine.getId();
            machine.sendEvent(Events.E23);
            final Machine<States, Events> instance = service.read(def, machineId);
            assertThat(instance.getCurrentState()).isEqualTo(States.S3).as("states match");
            assertThat(machineId).isEqualTo(fixedId).as("the desired ID was properly applied");
        } catch (Exception e) {
            fail("", e);
        }
    }

    @Test
    public void testStateMachineHelper_processNew() {
        // test a state machine is created and the new machine properly processed
        try {
            final Machine machine = service.newMachine(def, new HashMap<>()).start();
            final String machineId = machine.getId();
            machine.sendEvent(Events.E23);
            final Machine<States, Events> instance = service.read(def, machineId);
            assertThat(instance.getCurrentState()).isEqualTo(States.S3).as("states match");

            // test terminated machines service does not return false positives
            final List<MachineSnapshot> terminated0 = service.findTerminated();
            assertThat(terminated0).isEmpty();

            // test stale machines are found
            try {
                Thread.sleep(1200);
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
            final List<MachineSnapshot> stale = service.findStale(1);
            assertThat(stale.stream().map(MachineSnapshot::getId).collect(toList())).contains(instance.getId());

            // test the stale machine service does not return false positives
            final List<MachineSnapshot> stale2 = service.findStale(60);
            assertThat(stale2).isEmpty();

            // test machine is properly updated
            final Machine<States, Events> instance2 = service.read(def, machineId);
            instance2.sendEvent(Events.E34);
            final Machine<States, Events> updated = service.read(def, machineId);
            assertThat(updated.getCurrentState()).isEqualTo(States.S4).as("states match");

            // test terminated machines are properly found
            final List<MachineSnapshot> terminated = service.findTerminated();
            assertThat(terminated).hasSize(1).extracting("id").containsExactly(machineId);
        } catch (Exception e) {
            fail("", e);
        }
    }
}
