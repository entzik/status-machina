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

package io.statusmachina.handler.springjpa;

import com.google.common.collect.ImmutableMap;
import io.statusmachina.TestSpringBootApp;
import io.statusmachina.core.MachineDefImpl;
import io.statusmachina.core.MachineInstanceImpl;
import io.statusmachina.core.Transition;
import io.statusmachina.core.TransitionException;
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.Machine;
import io.statusmachina.core.api.TransitionAction;
import io.statusmachina.core.spi.StateMachineLockService;
import io.statusmachina.core.spi.StateMachineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static io.statusmachina.core.Transition.event;
import static io.statusmachina.core.Transition.stp;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = TestSpringBootApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Transactional
public class SpringJpaStateMachineServiceTest {
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

    @Autowired
    StateMachineLockService lockService;

    @Test
    void testSaveStateMachine() {
        try {
            final Machine<States, Events> instance = buildStateMachine();
            service.create(instance);

            final Machine<States, Events> read = service.read(def, instance.getId());

            assertThat(read.getId()).isEqualTo(instance.getId()).as("id matches");
            assertThat(read.getContext()).containsExactly(instance.getContext().entrySet().toArray(new Map.Entry[instance.getContext().size()])).as("context matches");
            assertThat(read.getCurrentState()).isEqualTo(instance.getCurrentState()).as("states match");

        } catch (TransitionException e) {
            fail("machine was not created", e);
        }
    }

    @Test
    void testUpdateStateMachine() {
        try {
            // create a state machine instance
            final Machine<States, Events> instance = buildStateMachine();
            service.create(instance);
            lockService.release(instance.getId());

            // lock / read / send event / update / releaase
            lockService.lock(instance.getId());
            final Machine<States, Events> created = service.read(def, instance.getId());
            final Machine<States, Events> tbu = created.sendEvent(Events.E23);
            service.update(tbu);
            lockService.release(instance.getId());

            // read updated state machine from DB
            final Machine<States, Events> updated = service.read(def, instance.getId());

            // assert
            assertThat(updated.getId()).isEqualTo(instance.getId()).as("id matches");
            assertThat(updated.getContext()).containsExactly(instance.getContext().entrySet().toArray(new Map.Entry[instance.getContext().size()])).as("context matches");
            assertThat(updated.getCurrentState()).isEqualTo(States.S3).as("states match");

        } catch (TransitionException e) {
            fail("machine was not created", e);
        }
    }

    @Test
    void testStateMachineLockedAfterSaving() {
        try {
            final Machine<States, Events> instance = buildStateMachine();
            service.create(instance);

            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> lockService.lock(instance.getId()))
                    .withMessageStartingWith("machine is locked by another instance, ID=")
                    .as("new machines are locked by default, locking again should have thrown IllegalStateException");
        } catch (TransitionException e) {
            fail("machine was not created", e);
        }
    }

    @Test
    void testUnclockAndLockBack() {
        try {
            final Machine<States, Events> instance = buildStateMachine();
            service.create(instance);

            // a new machine is locked so unlock it
            lockService.release(instance.getId());

            // now lock it back
            lockService.lock(instance.getId());

            // and make sure you can't lock it twice
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> lockService.lock(instance.getId()))
                    .withMessageStartingWith("machine is locked by another instance, ID=")
                    .as("new machines are locked by default, locking again should have thrown IllegalStateException");
        } catch (TransitionException e) {
            fail("machine was not created", e);
        }
    }

    private Machine<States, Events> buildStateMachine() throws TransitionException {
        final HashMap<String, String> context = new HashMap<>();
        context.put("k1", "v1");
        context.put("k2", "v2");
        context.put("k3", "v3");
        return MachineInstanceImpl.ofType(def).withContext(context).build();
    }


    enum States {
        S1, S2, S3, S4, S5
    }

    enum Events {
        E23, E34, E35
    }

    static class SpyAction<P> implements TransitionAction<P> {
        private boolean beenThere = false;
        private ImmutableMap<String, String> context;
        private P p;


        public boolean hasBeenThere() {
            return beenThere;
        }

        public Map<String, String> getContext() {
            return context;
        }

        public void reset() {
            beenThere = false;
        }

        @Override
        public ImmutableMap<String, String> apply(ImmutableMap<String, String> context, P p) {
            this.context = context;
            this.p = p;
            this.beenThere = true;
            return this.context;
        }
    }

}
