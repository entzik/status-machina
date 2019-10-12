package io.statusmachina.handler.spring.atomix;

import com.google.common.collect.ImmutableMap;
import io.statusmachina.TestSpringBootApp;
import io.statusmachina.core.MachineDefImpl;
import io.statusmachina.core.MachineInstanceImpl;
import io.statusmachina.core.Transition;
import io.statusmachina.core.TransitionException;
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.Machine;
import io.statusmachina.core.api.TransitionAction;
import io.statusmachina.core.spi.StateMachineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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

    final Transition<States, Events> t1 = Transition.stp(States.S1, States.S2, a1);
    final Transition<States, Events> t2 = Transition.event(States.S2, States.S3, Events.E23, a2);
    final Transition<States, Events> t3 = Transition.event(States.S3, States.S4, Events.E34, a3);
    final Transition<States, Events> t4 = Transition.event(States.S3, States.S5, Events.E35, a4);

    final MachineDefinition<States, Events> def = MachineDefImpl.<States, Events>newBuilder()
            .setName("toto")
            .states(States.values())
            .initialState(States.S1)
            .terminalStates(States.S4, States.S5)
            .events(Events.values())
            .transitions(t1, t2, t3, t4)
            .eventToString(new EventsStringFunction())
            .stringToEvent(new StringEventsFunction())
            .stateToString(new StatesStringFunction())
            .stringToState(new StringStatesFunction())
            .build();

    @Autowired
    StateMachineService service;

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

            // lock / read / send event / update / releaase
            final Machine<States, Events> created = service.read(def, instance.getId());
            created.sendEvent(Events.E23);
            service.update(created);

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


    public static class EventsStringFunction implements Function<Events, String> {
        @Override
        public String apply(Events events) {
            return events.name();
        }
    }

    public static class StringEventsFunction implements Function<String, Events> {
        @Override
        public Events apply(String s) {
            return Events.valueOf(s);
        }
    }

    public static class StatesStringFunction implements Function<States, String> {
        @Override
        public String apply(States states) {
            return states.name();
        }
    }

    public static class StringStatesFunction implements Function<String, States> {
        @Override
        public States apply(String s) {
            return States.valueOf(s);
        }
    }
}
