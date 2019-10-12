package io.statusmachina.core;

import com.google.common.collect.ImmutableMap;
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.Machine;
import io.statusmachina.core.api.TransitionAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.statusmachina.core.Transition.event;
import static io.statusmachina.core.Transition.stp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


public class MachineInstanceTest {
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
            .errorHandler(statesEventsErrorData -> {})
            .eventToString(Enum::name)
            .stringToEvent(Events::valueOf)
            .stateToString(Enum::name)
            .stringToState(States::valueOf)
            .build();

    @BeforeEach
    void before() {
        a1.reset();
        a2.reset();
        a3.reset();
        a4.reset();
    }

    @Test
    void testInstantiationAndStp() {
        try {
            final Machine<States, Events> instance = new MachineInstanceImpl<>(def, new HashMap<>());
            assertThat(instance.getId()).isNotEmpty();
            assertThat(instance.getCurrentState()).isEqualTo(States.S2).as("after creation machine has moved from state S1 to state S2 using STP transition t1");
            assertThat(a1.hasBeenThere()).isTrue();
        } catch (TransitionException e) {
            fail("machine was not instantiated", e);
        }
    }

    @Test
    void testEventTransition1() {
        try {
            final MachineInstanceImpl<States, Events> instance = new MachineInstanceImpl<>(def, new HashMap<>());
            final Machine<States, Events> updated = instance.sendEvent(Events.E23);
            assertThat(updated.getCurrentState()).isEqualTo(States.S3).as("after creation machine has moved from state S2 to state S3 using event transition t2");
            assertThat(a1.hasBeenThere()).isTrue();
            assertThat(a2.hasBeenThere()).isTrue();
        } catch (TransitionException e) {
            fail("machine was not instantiated", e);
        }
    }

    @Test
    void testEventTransition2() {
        try {
            final MachineInstanceImpl<States, Events> instance = new MachineInstanceImpl<>(def, new HashMap<>());
            final Machine<States, Events> updated1 = instance.sendEvent(Events.E23);
            final Machine<States, Events> updated2 = updated1.sendEvent(Events.E34);
            assertThat(updated2.getCurrentState()).isEqualTo(States.S4).as("after creation machine has moved from state S3 to state S4 using event transition t3");
            assertThat(a1.hasBeenThere()).isTrue();
            assertThat(a2.hasBeenThere()).isTrue();
            assertThat(a3.hasBeenThere()).isTrue();
            assertThat(a4.hasBeenThere()).isFalse();
        } catch (TransitionException e) {
            fail("machine was not instantiated", e);
        }
    }

    @Test
    void testEventTransition3() {
        try {
            final MachineInstanceImpl<States, Events> instance = new MachineInstanceImpl<>(def, new HashMap<>());
            final Machine<States, Events> updated1 = instance.sendEvent(Events.E23);
            final Machine<States, Events> updated2 = updated1.sendEvent(Events.E35);
            assertThat(updated2.getCurrentState()).isEqualTo(States.S5).as("after creation machine has moved from state S3 to state S5 using event transition t4");
            assertThat(a1.hasBeenThere()).isTrue();
            assertThat(a2.hasBeenThere()).isTrue();
            assertThat(a3.hasBeenThere()).isFalse();
            assertThat(a4.hasBeenThere()).isTrue();
        } catch (TransitionException e) {
            fail("machine was not instantiated", e);
        }
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
