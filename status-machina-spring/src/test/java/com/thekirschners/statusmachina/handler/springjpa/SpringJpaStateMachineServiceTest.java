package com.thekirschners.statusmachina.handler.springjpa;

import com.thekirschners.statusmachina.TestSpringBootApp;
import com.thekirschners.statusmachina.core.MachineDef;
import com.thekirschners.statusmachina.core.MachineInstance;
import com.thekirschners.statusmachina.core.Transition;
import com.thekirschners.statusmachina.core.TransitionException;
import com.thekirschners.statusmachina.handler.StateMachineLockService;
import com.thekirschners.statusmachina.handler.StateMachineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = TestSpringBootApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
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
    StateMachineService service;

    @Autowired
    StateMachineLockService lockService;

    @Test
    void testSaveStateMachine() {
        try {
            final MachineInstance<States, Events> instance = buildStateMachine();
            service.create(instance);

            final MachineInstance<States, Events> read = service.read(def, instance.getId());

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
            final MachineInstance<States, Events> instance = buildStateMachine();
            service.create(instance);
            lockService.release(instance.getId());

            // lock / read / send event / update / releaase
            lockService.lock(instance.getId());
            final MachineInstance<States, Events> created = service.read(def, instance.getId());
            created.sendEvent(Events.E23);
            service.update(created);
            lockService.release(instance.getId());

            // read updated state machine from DB
            final MachineInstance<States, Events> updated = service.read(def, instance.getId());

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
            final MachineInstance<States, Events> instance = buildStateMachine();
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
            final MachineInstance<States, Events> instance = buildStateMachine();
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

    private MachineInstance<States, Events> buildStateMachine() throws TransitionException {
        final HashMap<String, String> context = new HashMap<>();
        context.put("k1", "v1");
        context.put("k2", "v2");
        context.put("k3", "v3");
        return (MachineInstance<States, Events>) MachineInstance.ofType(def).withContext(context);
    }


    enum States {
        S1, S2, S3, S4, S5
    }

    enum Events {
        E23, E34, E35
    }

    static class SpyAction implements Consumer<Map<String, String>> {
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
    }
}
