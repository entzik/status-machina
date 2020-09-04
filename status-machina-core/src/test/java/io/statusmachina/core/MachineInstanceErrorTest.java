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
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class MachineInstanceErrorTest {
    final SpyAction a1 = new SpyAction() {
        @Override
        public ImmutableMap<String, String> apply(ImmutableMap context, Object o) {
            stash("toto", "titi");
            throw new IllegalStateException("some action error");
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
            .errorHandler(statesEventsErrorData -> {
            })
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
    void testStateMachineInErrorFollowingTransitionActionfailure() {
        Machine<States, Events> instance = null;
        try {
            instance = new MachineInstanceImpl<>(def, machinePersistenceCallback, new HashMap<>());
            instance = instance.start();
            assertThat(instance.getId()).isNotEmpty();
            assertThat(instance.isErrorState()).isTrue();
            assertThat(instance.getError()).isNotEmpty().contains("some action error");
        } catch (Exception e) {
            fail("", e);
        }
    }

    @Test
    void testSendEventToMachineInErrorState() {
        Machine<States, Events> instance = null;
        try {
            instance = new MachineInstanceImpl<>(def, machinePersistenceCallback, new HashMap<>());
            instance = instance.start();
            Machine<States, Events> crt = instance;
            assertThatThrownBy(() -> crt.sendEvent(Events.E23))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("a state machine cannot accept event when in error state:  type " + def.getName()+", id " + crt.getId() + "  error some action error");

        } catch (Exception e) {
            fail("", e);
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
