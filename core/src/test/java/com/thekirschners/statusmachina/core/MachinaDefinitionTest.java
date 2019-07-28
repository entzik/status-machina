package com.thekirschners.statusmachina.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MachinaDefinitionTest {
    final Transition<States, Events> t1 = new Transition<>(States.S1, States.S2);
    final Transition<States, Events> t2 = new Transition<>(States.S2, States.S3, Events.E23);
    final Transition<States, Events> t3 = new Transition<>(States.S3, States.S4, Events.E34);
    final Transition<States, Events> t4 = new Transition<>(States.S3, States.S5, Events.E35);

    final MachineDef<States, Events> def = MachineDef.<States, Events>newBuilder()
            .setName("toto")
            .states(States.values())
            .initialState(States.S1)
            .terminalStates(States.S4, States.S5)
            .events(Events.values())
            .transitions(t1, t2, t3, t4)
            .build();

    @Test
    void testTransitions() {
        assertThat(t1.isSTP()).isTrue().as("transition t1 correctly identified as STP");
        assertThat(t1.getFrom()).isEqualTo(States.S1).as("transition t1 has source state properly identified");
        assertThat(t1.getTo()).isEqualTo(States.S2).as("transition t1 has source state properly identified");
        assertThat(t2.isSTP()).isFalse().as("transition t2 correctly identified as not STP");
        assertThat(t2.getFrom()).isEqualTo(States.S2).as("transition t2 has source state properly identified");
        assertThat(t2.getTo()).isEqualTo(States.S3).as("transition t2 has source state properly identified");
        assertThat(t2.getEvent().get()).isEqualTo(Events.E23).as("transition t2 has event properly identified");
    }

    @Test
    void testValidDef() {
        assertThat(def).isNotNull().as("a state machine definition was created");
        assertThat(def.getName()).isEqualTo("toto").as("state machine definition bares expected name");
        assertThat(def.getAllStates()).containsOnly(States.values()).as("state machine definition was correctly configured with all states");
        assertThat(def.getInitialState()).isEqualTo(States.S1).as("state machine configured with expected initial state");
        assertThat(def.getTerminalStates()).containsOnly(States.S4, States.S5).as("state machine configured with expected terminal states");
    }

    enum States {
        S1, S2, S3, S4, S5
    }

    enum Events {
        E23, E34, E35
    }
}
