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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.statusmachina.core.api.Transition.event;
import static io.statusmachina.core.api.Transition.stp;
import static org.assertj.core.api.Assertions.*;


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
    final SpyAction a5 = new SpyAction() {
        @Override
        public ImmutableMap<String, String> apply(ImmutableMap context, Object o) {
            throw new IllegalStateException("exception during action");
        }
    };

    final SpyPostAction a5Post = new SpyPostAction() {
        @Override
        public void accept(ImmutableMap immutableMap, Object o) {
            throw new IllegalStateException("exception during post action");
        }
    };


    final Transition<States, Events> t1 = stp(States.S1, States.S1a, a1, a1Post);
    final Transition<States, Events> t11 = stp(States.S1a, States.S2, a1, a1Post);
    final Transition<States, Events> t2 = event(States.S2, States.S3, Events.E23, a2);
    final Transition<States, Events> t2c6 = event(States.S2, States.S3, Events.E23, a2, () -> 6L);
    final Transition<States, Events> t3 = event(States.S3, States.S4, Events.E34, a3, a3Post);
    final Transition<States, Events> t4 = event(States.S3, States.S5, Events.E35, a4);
    final Transition<States, Events> t5 = event(States.S3, States.S6, Events.E36, a5, a5Post);
    final Transition<States, Events> t6 = event(States.S3, States.S7, Events.E37, a4, a5Post);


    final MachinePersistenceCallback<States, Events> machinePersistenceCallback = new MachinePersistenceCallback<>() {
        @Override
        public Machine<States, Events> saveNew(Machine<States, Events> machine) {
            return null;
        }

        @Override
        public Machine<States, Events> update(Machine<States, Events> machine, long epochMilliForUpdate) {
            return machine;
        }

        @Override
        public <R> R runInTransaction(Callable<R> callable) throws Exception {
            return callable.call();
        }
    };

    List<ErrorData> errorDataList = new ArrayList<>();
    List<TransitionData> transitionsDataList = new ArrayList<>();


    final MachineDefinition<States, Events> def = new EnumBasedMachineDefinitionBuilderProvider().getMachineDefinitionBuilder(MachinaDefinitionTest.States.class, MachinaDefinitionTest.Events.class)
            .name("toto")
            .states(States.values())
            .initialState(States.S1)
            .terminalStates(States.S4, States.S5)
            .events(Events.values())
            .transitions(t1, t11, t2, t3, t4, t5, t6)
            .errorHandler(statesEventsErrorData -> {
                errorDataList.add((ErrorData) statesEventsErrorData);
            })
            .transitionHandler(transitionData -> {
                transitionsDataList.add((TransitionData) transitionData);
            })
            .build();

    final MachineDefinition<States, Events> defCardinality = new EnumBasedMachineDefinitionBuilderProvider().getMachineDefinitionBuilder(MachinaDefinitionTest.States.class, MachinaDefinitionTest.Events.class)
            .name("toto")
            .states(States.values())
            .initialState(States.S1)
            .terminalStates(States.S4, States.S5)
            .events(Events.values())
            .transitions(t1, t11, t2c6, t3, t4, t5, t6)
            .errorHandler(statesEventsErrorData -> {
                errorDataList.add((ErrorData) statesEventsErrorData);
            })
            .transitionHandler(transitionData -> {
                transitionsDataList.add((TransitionData) transitionData);
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
        errorDataList.clear();
        transitionsDataList.clear();
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
            assertThat(errorDataList).isEmpty();
            assertThat(transitionsDataList).hasSize(1);
            assertThat(transitionsDataList.get(0).getStateMachineId()).isEqualTo(instance.getId());
            assertThat(transitionsDataList.get(0).getStateMachineType()).isEqualTo("toto");
            assertThat(transitionsDataList.get(0).getFrom()).isEqualTo(States.S1a);
            assertThat(transitionsDataList.get(0).getTo()).isEqualTo(States.S2);
            assertThat(transitionsDataList.get(0).getLastModifiedEpoch()).isNotNull();
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
    void testEventTransition1Cardinality6() {
        try {
            final Machine<States, Events> instance = new MachineInstanceImpl<>(defCardinality, machinePersistenceCallback, new HashMap<>()).start();
            Machine<States, Events> updated = instance;

            updated = updated.sendEvent(Events.E23);
            assertThat(updated.getTransitionEventCounter()).isEqualTo(1);
            assertThat(updated.getCurrentState()).isEqualTo(States.S2).as("event delivered 1 time(s) stay in s2");

            updated = updated.sendEvent(Events.E23);
            assertThat(updated.getTransitionEventCounter()).isEqualTo(2);
            assertThat(updated.getCurrentState()).isEqualTo(States.S2).as("event delivered 1 time(s) stay in s2");

            updated = updated.sendEvent(Events.E23);
            assertThat(updated.getTransitionEventCounter()).isEqualTo(3);
            assertThat(updated.getCurrentState()).isEqualTo(States.S2).as("event delivered 1 time(s) stay in s2");

            updated = updated.sendEvent(Events.E23);
            assertThat(updated.getTransitionEventCounter()).isEqualTo(4);
            assertThat(updated.getCurrentState()).isEqualTo(States.S2).as("event delivered 1 time(s) stay in s2");

            updated = updated.sendEvent(Events.E23);
            assertThat(updated.getTransitionEventCounter()).isEqualTo(5);
            assertThat(updated.getCurrentState()).isEqualTo(States.S2).as("event delivered 1 time(s) stay in s2");

            updated = updated.sendEvent(Events.E23);
            assertThat(updated.getTransitionEventCounter()).isEqualTo(0);
            assertThat(updated.getCurrentState()).isEqualTo(States.S3).as("after creation machine has moved from state S2 to state S3 using event transition t2");

            assertThat(a1.hasBeenThere()).isTrue();
            assertThat(a2.hasBeenThere()).isTrue();
        } catch (Exception e) {
            fail("machine was not instantiated", e);
        }
    }

    @Test
    void testEventTransition1CardinalityError() {
        try {
            final Machine<States, Events> instance = new MachineInstanceImpl<>(defCardinality, machinePersistenceCallback, new HashMap<>()).start();
            Machine<States, Events> updated = instance;

            updated = updated.sendEvent(Events.E23);
            assertThat(updated.getTransitionEventCounter()).isEqualTo(1);
            assertThat(updated.getCurrentState()).isEqualTo(States.S2).as("event delivered 1 time(s) stay in s2");

            updated = updated.sendEvent(Events.E23);
            assertThat(updated.getTransitionEventCounter()).isEqualTo(2);
            assertThat(updated.getCurrentState()).isEqualTo(States.S2).as("event delivered 1 time(s) stay in s2");

            updated = updated.sendEvent(Events.E23);
            assertThat(updated.getTransitionEventCounter()).isEqualTo(3);
            assertThat(updated.getCurrentState()).isEqualTo(States.S2).as("event delivered 1 time(s) stay in s2");

            updated = updated.sendEvent(Events.E34);
            fail("should have thrown an exception because the event cardinality is 6 and the 3rd event was not the expeacted one");
        } catch (Exception e) {
            // expected
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

    @Test
    void testErrorWhenEventReceivedInTerminalState() {
        try {
            final Machine<States, Events> instance = new MachineInstanceImpl<States, Events>(def, machinePersistenceCallback, new HashMap<>()).start();
            final Machine<States, Events> updated1 = instance.sendEvent(Events.E23);
            final Machine<States, Events> updated2 = updated1.sendEvent(Events.E35);

            assertThatThrownBy(() -> updated2.sendEvent(Events.E23))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("java.lang.IllegalStateException: state machine of type " + def.getName() + " with ID " + updated2.getId() + " event " + Events.E23.toString() + " has received an event while in ternminal state " + States.S5 + ". Aborting.");
        } catch (Exception e) {
            fail("machine was not instantiated", e);
        }
    }

    @Test
    void testErrorWhenEventReceivedInTerminalState2() {
        try {
            final Machine<States, Events> instance = new MachineInstanceImpl<States, Events>(def, machinePersistenceCallback, new HashMap<>()).start();
            final Machine<States, Events> updated1 = instance.sendEvent(Events.E23);
            final Machine<States, Events> updated2 = updated1.sendEvent(Events.E35);

            assertThatThrownBy(() -> updated2.sendEvent(Events.E23, "xyz"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("java.lang.IllegalStateException: state machine of type " + def.getName() + " with ID " + updated2.getId() + " event " + Events.E23.toString() + " has received an event while in ternminal state " + States.S5 + ". Aborting.");
        } catch (Exception e) {
            fail("machine was not instantiated", e);
        }
    }

    @Test
    void testErrorDuringAction() {
        try {
            final Machine<States, Events> instance = new MachineInstanceImpl<States, Events>(def, machinePersistenceCallback, new HashMap<>()).start();
            final Machine<States, Events> updated1 = instance.sendEvent(Events.E23);
            final Machine<States, Events> updated2 = updated1.sendEvent(Events.E36);

            assertThat(errorDataList).hasSize(1);
            assertThat(errorDataList.get(0).getStateMachineId()).isEqualTo(instance.getId());
            assertThat(errorDataList.get(0).getStateMachineType()).isEqualTo("toto");
            assertThat(errorDataList.get(0).isPostActionError()).isEqualTo(false);
            assertThat(errorDataList.get(0).getLastModifiedEpoch()).isNotNull();
            assertThat(errorDataList.get(0).getFrom()).isEqualTo(States.S3);
            assertThat(errorDataList.get(0).getTo()).isEqualTo(States.S6);
            assertThat(errorDataList.get(0).getErrorMessage()).isEqualTo("exception during action");
            assertThat(errorDataList.get(0).getEvent().get()).isEqualTo(Events.E36);

            assertThat(transitionsDataList).hasSize(2);
            assertThat(transitionsDataList.get(0).getStateMachineId()).isEqualTo(instance.getId());
            assertThat(transitionsDataList.get(0).getStateMachineType()).isEqualTo("toto");
            assertThat(transitionsDataList.get(0).getFrom()).isEqualTo(States.S1a);
            assertThat(transitionsDataList.get(0).getTo()).isEqualTo(States.S2);
            assertThat(transitionsDataList.get(0).getLastModifiedEpoch()).isNotNull();

            assertThat(transitionsDataList.get(1).getStateMachineId()).isEqualTo(instance.getId());
            assertThat(transitionsDataList.get(1).getStateMachineType()).isEqualTo("toto");
            assertThat(transitionsDataList.get(1).getFrom()).isEqualTo(States.S2);
            assertThat(transitionsDataList.get(1).getTo()).isEqualTo(States.S3);
            assertThat(transitionsDataList.get(1).getLastModifiedEpoch()).isNotNull();

        } catch (Exception e) {
            fail("machine was not instantiated", e);
        }
    }

    @Test
    void testErrorDuringPostAction() {
        try {
            final Machine<States, Events> instance = new MachineInstanceImpl<States, Events>(def, machinePersistenceCallback, new HashMap<>()).start();
            final Machine<States, Events> updated1 = instance.sendEvent(Events.E23);
            assertThatThrownBy(() -> updated1.sendEvent(Events.E37))
                    .isInstanceOf(TransitionException.class);

            assertThat(errorDataList).hasSize(1);
            assertThat(errorDataList.get(0).getStateMachineId()).isEqualTo(instance.getId());
            assertThat(errorDataList.get(0).getStateMachineType()).isEqualTo("toto");
            assertThat(errorDataList.get(0).isPostActionError()).isEqualTo(true);
            assertThat(errorDataList.get(0).getLastModifiedEpoch()).isNotNull();
            assertThat(errorDataList.get(0).getFrom()).isEqualTo(States.S3);
            assertThat(errorDataList.get(0).getTo()).isEqualTo(States.S7);
            assertThat(errorDataList.get(0).getErrorMessage()).isEqualTo("exception during post action");
            assertThat(errorDataList.get(0).getEvent().get()).isEqualTo(Events.E37);

            assertThat(transitionsDataList).hasSize(3);
            assertThat(transitionsDataList.get(0).getStateMachineId()).isEqualTo(instance.getId());
            assertThat(transitionsDataList.get(0).getStateMachineType()).isEqualTo("toto");
            assertThat(transitionsDataList.get(0).getFrom()).isEqualTo(States.S1a);
            assertThat(transitionsDataList.get(0).getTo()).isEqualTo(States.S2);
            assertThat(transitionsDataList.get(0).getLastModifiedEpoch()).isNotNull();

            assertThat(transitionsDataList.get(1).getStateMachineId()).isEqualTo(instance.getId());
            assertThat(transitionsDataList.get(1).getStateMachineType()).isEqualTo("toto");
            assertThat(transitionsDataList.get(1).getFrom()).isEqualTo(States.S2);
            assertThat(transitionsDataList.get(1).getTo()).isEqualTo(States.S3);
            assertThat(transitionsDataList.get(1).getLastModifiedEpoch()).isNotNull();

            assertThat(transitionsDataList.get(2).getStateMachineId()).isEqualTo(instance.getId());
            assertThat(transitionsDataList.get(2).getStateMachineType()).isEqualTo("toto");
            assertThat(transitionsDataList.get(2).getFrom()).isEqualTo(States.S3);
            assertThat(transitionsDataList.get(2).getTo()).isEqualTo(States.S7);
            assertThat(transitionsDataList.get(2).getLastModifiedEpoch()).isNotNull();

        } catch (Exception e) {
            fail("machine was not instantiated", e);
        }
    }

    enum States {
        S1, S1a, S2, S3, S4, S5, S6, S7
    }

    enum Events {
        E23, E34, E35, E3, E36, E37
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
