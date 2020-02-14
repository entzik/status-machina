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

package some.unrelated.app.tests;

import io.statusmachina.core.api.Machine;
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.spi.StateMachineService;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = TestSpringBootApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class SpringJpaStateMachinePersistenceTest {

    @Autowired
    StateMachineService<States, Events> service;

    @Autowired
    @Qualifier("Test1StateMachineDef")
    MachineDefinition<States, Events> def;

    @Test
    void testSaveStateMachine() {
        try {
            final Machine<States, Events> instance = buildStateMachine();

            final Machine<States, Events> read = service.read(def, instance.getId());

            assertThat(read.getId()).isEqualTo(instance.getId()).as("id matches");
            assertThat(read.getContext()).containsExactly(instance.getContext().entrySet().toArray(new Map.Entry[instance.getContext().size()])).as("context matches");
            assertThat(read.getCurrentState()).isEqualTo(instance.getCurrentState()).as("states match");

        } catch (Exception e) {
            fail("machine was not created", e);
        }
    }

    @Test
    void testUpdateStateMachine() {
        try {
            // create a state machine instance
            final Machine<States, Events> instance = buildStateMachine().start();

            // lock / read / send event / update / releaase
            final Machine<States, Events> created = service.read(def, instance.getId());
            final Machine<States, Events> tbu = created.sendEvent(Events.E23);

            // read updated state machine from DB
            final Machine<States, Events> updated = service.read(def, instance.getId());

            // assert
            assertThat(updated.getId()).isEqualTo(instance.getId()).as("id matches");
            assertThat(updated.getContext()).containsExactly(instance.getContext().entrySet().toArray(new Map.Entry[instance.getContext().size()])).as("context matches");
            assertThat(updated.getCurrentState()).isEqualTo(States.S3).as("states match");

        } catch (Exception e) {
            fail("machine was not created", e);
        }
    }


    private Machine<TestOneStateMachineConfiguration.States, TestOneStateMachineConfiguration.Events> buildStateMachine() throws Exception {
        final HashMap<String, String> context = new HashMap<>();
        context.put("k1", "v1");
        context.put("k2", "v2");
        context.put("k3", "v3");
        return service.newMachine(def, context);
    }


}
