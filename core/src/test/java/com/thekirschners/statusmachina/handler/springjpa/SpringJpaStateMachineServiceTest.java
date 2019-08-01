package com.thekirschners.statusmachina.handler.springjpa;

import com.thekirschners.statusmachina.TestSpringBootApp;
import com.thekirschners.statusmachina.core.*;
import com.thekirschners.statusmachina.handler.springjpa.model.ExternalState;
import com.thekirschners.statusmachina.handler.springjpa.repo.ExternalStateRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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

    final Transition<States, Events> t1 = new Transition<>(States.S1, States.S2, a1);
    final Transition<States, Events> t2 = new Transition<>(States.S2, States.S3, Events.E23, a2);
    final Transition<States, Events> t3 = new Transition<>(States.S3, States.S4, Events.E34, a3);
    final Transition<States, Events> t4 = new Transition<>(States.S3, States.S5, Events.E35, a4);

    final MachineDef<States, Events> def = MachineDef.<States, Events>newBuilder()
            .setName("toto")
            .states(States.values())
            .initialState(States.S1)
            .terminalStates(States.S4, States.S5)
            .events(Events.values())
            .transitions(t1, t2, t3, t4)
            .setEventToString(Enum::name)
            .setStringToEvent(Events::valueOf)
            .setStateToString(Enum::name)
            .setStringToState(States::valueOf)
            .build();

    @Autowired
    SpringJpaStateMachineService service;

    @Test
    void springTest() {
        try {
            final HashMap<String, String> context = new HashMap<>();
            context.put("k1", "v1");
            context.put("k2", "v2");
            context.put("k3", "v3");
            final MachineInstance<States, Events> instance = MachineInstance.ofType(def).withContext(context);
            service.create(instance);

            final MachineInstance<States, Events> read = service.read(def, instance.getId());

            assertThat(read.getId()).isEqualTo(instance.getId()).as("id matches");
            assertThat(read.getContext()).containsExactly(context.entrySet().toArray(new Map.Entry[context.size()])).as("context matches");
            assertThat(read.getCurrentState()).isEqualTo(instance.getCurrentState()).as("states match");

        } catch (TransitionException e) {
            fail("machine was not created", e);
        }
    }


    enum States {
        S1, S2, S3, S4, S5
    }

    enum Events {
        E23, E34, E35
    }

    static class SpyAction implements Consumer<Map<String,String>> {
        private boolean beenThere = false;
        private Map<String, String> context;

        @Override
        public void accept(Map<String, String> stringStringMap) {
            this.context = stringStringMap;
            beenThere = true;
        }

        public boolean hasBeenThere() {
            return beenThere;
        }

        public Map<String, String> getContext() {
            return context;
        }

        public void reset() {
            beenThere = false;
        }
    }}
