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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import some.unrelated.app.TestSpringBootApp;
import some.unrelated.app.config.TestErrorStateMachineConfiguration.Events;
import some.unrelated.app.config.TestErrorStateMachineConfiguration.States;

import java.util.Map;
import java.util.UUID;

import static io.statusmachina.core.api.ErrorType.POST_TRANSITION;
import static io.statusmachina.core.api.ErrorType.TRANSITION;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = TestSpringBootApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class SpringStateMachineTransitionsErrorTest {

    @Autowired
    StateMachineService<States, Events> service;

    @Autowired
    @Qualifier("Test2StateMachineDef")
    MachineDefinition<States, Events> def1;

    @Autowired
    @Qualifier("Test3StateMachineDef")
    MachineDefinition<States, Events> def2;

    @Test
    public void testStateMachineHelper_errorInTransitionAction() {
        final String fixedId = UUID.randomUUID().toString();
        try {
            service.newMachine(def1, fixedId, Map.of()).start();
        } catch (Exception e) {
        }
        try {
            final Machine<States, Events> instance = service.read(def1, fixedId);
            assertThat(instance.getCurrentState()).isEqualTo(States.S2).as("states match");
            assertThat(instance.isErrorState()).isTrue();
            assertThat(instance.getErrorType()).isEqualTo(TRANSITION);
            assertThat(instance.getError().get()).isEqualTo("whatever");
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    @Test
    public void testStateMachineHelper_errorInPostTransitionAction() {
        final String fixedId = UUID.randomUUID().toString();
        try {
            service.newMachine(def2, fixedId, Map.of()).start();
        } catch (Exception e) {
        }
        try {
            final Machine<States, Events> instance = service.read(def1, fixedId);
            assertThat(instance.getCurrentState()).isEqualTo(States.S2).as("states match");
            assertThat(instance.isErrorState()).isTrue();
            assertThat(instance.getErrorType()).isEqualTo(POST_TRANSITION);
            assertThat(instance.getError().get()).isEqualTo("post whatever");
        } catch (Exception e) {
            fail(e.toString());
        }
    }

}
