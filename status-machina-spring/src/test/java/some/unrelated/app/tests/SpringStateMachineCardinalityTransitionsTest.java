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
import io.statusmachina.core.spi.StateMachineService;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import some.unrelated.app.TestSpringBootApp;
import some.unrelated.app.config.TestCardinalityStateMachineConfiguration;
import some.unrelated.app.config.TestCardinalityStateMachineConfiguration.Events;
import some.unrelated.app.config.TestCardinalityStateMachineConfiguration.States;
import some.unrelated.app.config.TestOneStateMachineConfiguration;

import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static some.unrelated.app.config.TestCardinalityStateMachineConfiguration.CARDINALITY;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = TestSpringBootApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@AutoConfigureEmbeddedDatabase(
        type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES,
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
)
public class SpringStateMachineCardinalityTransitionsTest {

    @Autowired
    StateMachineService<States, Events> service;

    @Autowired
    @Qualifier("TestCardinalityStateMachineDef")
    MachineDefinition<States, Events> def;


    @Test
    public void testStateMachineHelper_errorInTransitionAction() {
        final String fixedId = UUID.randomUUID().toString();
        try {
            String machineId;
            {
                final Machine machine = service.newMachine(def, fixedId, new HashMap<>()).start();
                machineId = machine.getId();
            }

            for (int i = 0; i < CARDINALITY - 1; i ++) {
                final Machine<States, Events> machine = service.read(def, machineId);
                assertThat(machine.getTransitionEventCounter()).isEqualTo(i);
                machine.sendEvent(Events.E23);
                final Machine<States, Events> instance = service.read(def, machineId);
                assertThat(instance.getTransitionEventCounter()).isEqualTo(i + 1);
                assertThat(instance.getCurrentState()).isEqualTo(States.S2).as("states match");
            }

            {
                final Machine<States, Events> machine = service.read(def, machineId);
                assertThat(machine.getTransitionEventCounter()).isEqualTo(CARDINALITY - 1);
                machine.sendEvent(Events.E23);
                final Machine<States, Events> instance = service.read(def, machineId);
                assertThat(instance.getTransitionEventCounter()).isEqualTo(0);
                assertThat(instance.getCurrentState()).isEqualTo(States.S3).as("states match");
            }

            assertThat(machineId).isEqualTo(fixedId).as("the desired ID was properly applied");
        } catch (Exception e) {
            fail("", e);
        }
    }
}
