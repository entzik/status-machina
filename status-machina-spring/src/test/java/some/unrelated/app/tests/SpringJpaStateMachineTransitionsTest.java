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

import io.statusmachina.core.api.Machine;
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.MachineSnapshot;
import io.statusmachina.core.spi.StateMachineService;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import some.unrelated.app.TestSpringBootApp;
import some.unrelated.app.config.TestOneStateMachineConfiguration;
import some.unrelated.app.config.TestOneStateMachineConfiguration.Events;
import some.unrelated.app.config.TestOneStateMachineConfiguration.States;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = TestSpringBootApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@AutoConfigureEmbeddedDatabase(
        type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES,
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
)
public class SpringJpaStateMachineTransitionsTest {

    @Autowired
    StateMachineService<TestOneStateMachineConfiguration.States, TestOneStateMachineConfiguration.Events> service;

    @Autowired
    @Qualifier("Test1StateMachineDef")
    MachineDefinition<TestOneStateMachineConfiguration.States, TestOneStateMachineConfiguration.Events> def;

    @Test
    public void testStateMachineHelper_newMachineWithId_startAgain() {
        final String fixedId = UUID.randomUUID().toString();
        try {
            service.newMachine(def, fixedId, Map.of()).start();
            final Machine<States, Events> instance = service.read( def, fixedId);
            assertThat(instance.getCurrentState()).isEqualTo(States.S2).as("states match");
            assertThatThrownBy(instance::start).isInstanceOf(IllegalStateException.class).hasMessageContaining("machine is already started");
        } catch (Exception e) {
            fail("", e);
        }
    }

    @Test
    public void testStateMachineHelper_reinitMachineWithId() {
        final String fixedId = UUID.randomUUID().toString();
        try {
            service.newMachine(def, fixedId, Map.of()).start();
            final Machine<States, Events> instance = service.read( def, fixedId);
            assertThat(instance.getCurrentState()).isEqualTo(States.S2).as("states match");
            service.newMachine(def, fixedId, Map.of()).start();
            final Machine<States, Events> instance2 = service.read( def, fixedId);
            assertThat(instance2.getCurrentState()).isEqualTo(States.S2).as("states match");
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
