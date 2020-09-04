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

import com.google.common.collect.ImmutableMap;
import io.statusmachina.core.api.*;
import io.statusmachina.core.spi.MachinePersistenceCallback;
import io.statusmachina.core.stdimpl.EnumBasedMachineDefinitionBuilderProvider;
import io.statusmachina.core.stdimpl.MachineInstanceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.statusmachina.core.api.Transition.event;
import static io.statusmachina.core.api.Transition.stp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


public class MachineInstanceTest {
    final SpyAction a1 = new SpyAction() {
        @Override
        public ImmutableMap<String, String> apply(ImmutableMap context, Object o) {
            stash("toto", "titi");
            return super.apply(context, o);
        }
    };
    final SpyPostAction a1Post = new SpyPostAction();
    final SpyAction a2 = new SpyAction();
    final SpyAction a3 = new SpyAction();
    final SpyPostAction a3Post = new SpyPostAction();
    final SpyAction a4 = new SpyAction();

    final Transition<States, Events> t1 = stp(States.S1, States.S2, a1, a1Post);
    final Transition<States, Events> t2 = event(States.S2, States.S3, Events.E23, a2);
    final Transition<States, Events> t3 = event(States.S3, States.S4, Events.E34, a3, a3Post);
    final Transition<States, Events> t4 = event(States.S3, States.S5, Events.E35, a4);

    final MachinePersistenceCallback<States, Events> machinePersistenceCallback = new MachinePersistenceCallback<>() {
        @Override
        public Machine<States, Events> saveNew(Machine<States, Events> machine) {
            return null;
        }

        @Override
        public Machine<States, Events> update(Machine<States, Events> machine) {
            return machine;
        }

        @Override
        public <R> R runInTransaction(Callable<R> callable) throws Exception {
            return callable.call();
        }
    };

    final MachineDefinition<States, Events> def = new EnumBasedMachineDefinitionBuilderProvider().getMachineDefinitionBuilder(MachinaDefinitionTest.States.class, MachinaDefinitionTest.Events.class)
            .name("toto")
            .states(States.values())
            .initialState(States.S1)
            .terminalStates(States.S4, States.S5)
            .events(Events.values())
            .transitions(t1, t2, t3, t4)
            .errorHandler(statesEventsErrorData -> {})
            .build();

    @BeforeEach
    void before() {
        a1.reset();
        a1Post.reset();
        a2.reset();
        a3.reset();
        a3Post.reset();
        a4.reset();
    }

    @Test
    void testInstantiationAndStp() {
        try {
            final Machine<States, Events> instance = new MachineInstanceImpl<>(def, machinePersistenceCallback, new HashMap<>()).start();
            assertThat(instance.getId()).isNotEmpty();
            assertThat(instance.getCurrentState()).isEqualTo(States.S2).as("after creation machine has moved from state S1 to state S2 using STP transition t1");
            assertThat(a1.hasBeenThere()).isTrue();
            assertThat(a1Post.hasBeenThere()).isTrue();
            assertThat(a1Post.getStashed()).isEqualTo("titi");
        } catch (Exception e) {
            fail("machine was not instantiated", e);
        }
    }

    @Test
    void testEventTransition1() {
        try {
            final Machine<States, Events> instance = new MachineInstanceImpl<>(def, machinePersistenceCallback, new HashMap<>()).start();
            final Machine<States, Events> updated = instance.sendEvent(Events.E23);
            assertThat(updated.getCurrentState()).isEqualTo(States.S3).as("after creation machine has moved from state S2 to state S3 using event transition t2");
            assertThat(a1.hasBeenThere()).isTrue();
            assertThat(a2.hasBeenThere()).isTrue();
        } catch (Exception e) {
            fail("machine was not instantiated", e);
        }
    }

    @Test
    void testEventTransition2() {
        try {
            final Machine<States, Events> instance = new MachineInstanceImpl<>(def, machinePersistenceCallback, new HashMap<>()).start();
            final Machine<States, Events> updated1 = instance.sendEvent(Events.E23);
            final Machine<States, Events> updated2 = updated1.sendEvent(Events.E34);
            assertThat(updated2.getCurrentState()).isEqualTo(States.S4).as("after creation machine has moved from state S3 to state S4 using event transition t3");
            assertThat(a1.hasBeenThere()).isTrue();
            assertThat(a1Post.hasBeenThere()).isTrue();
            assertThat(a2.hasBeenThere()).isTrue();
            assertThat(a3.hasBeenThere()).isTrue();
            assertThat(a3Post.hasBeenThere()).isTrue();
            assertThat(a4.hasBeenThere()).isFalse();
        } catch (Exception e) {
            fail("machine was not instantiated", e);
        }
    }

    @Test
    void testEventTransition3() {
        try {
            final Machine<States, Events> instance = new MachineInstanceImpl<States, Events>(def, machinePersistenceCallback, new HashMap<>()).start();
            final Machine<States, Events> updated1 = instance.sendEvent(Events.E23);
            final Machine<States, Events> updated2 = updated1.sendEvent(Events.E35);
            assertThat(updated2.getCurrentState()).isEqualTo(States.S5).as("after creation machine has moved from state S3 to state S5 using event transition t4");
            assertThat(a1.hasBeenThere()).isTrue();
            assertThat(a1Post.hasBeenThere()).isTrue();
            assertThat(a2.hasBeenThere()).isTrue();
            assertThat(a3.hasBeenThere()).isFalse();
            assertThat(a3Post.hasBeenThere()).isFalse();
            assertThat(a4.hasBeenThere()).isTrue();
        } catch (Exception e) {
            fail("machine was not instantiated", e);
        }
    }

    enum States {
        S1, S2, S3, S4, S5
    }

    enum Events {
        E23, E34, E35
    }

    static class SpyAction<P> extends TransitionActionBase<P> {
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

    static class SpyPostAction<P> implements TransitionPostAction<P> {
        private boolean beenThere = false;
        private ImmutableMap<String, String> context;
        private P p;
        private String stashed;


        public boolean hasBeenThere() {
            return beenThere;
        }

        public Map<String, String> getContext() {
            return context;
        }

        public void reset() {
            beenThere = false;
            stashed = null;
        }

        public String getStashed() {
            return stashed;
        }

        @Override
        public void accept(ImmutableMap<String, String> stringStringImmutableMap, P p) {
            stashed = getStash("toto", String.class);
            this.context = context;
            this.p = p;
            this.beenThere = true;
        }
    }
}
