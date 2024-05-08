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

package some.unrelated.app.config;

import com.google.common.collect.ImmutableMap;
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.MachineDefinitionBuilderProvider;
import io.statusmachina.core.api.Transition;
import io.statusmachina.core.api.TransitionActionBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static io.statusmachina.core.api.Transition.event;
import static io.statusmachina.core.api.Transition.stp;

@Configuration
public class TestCardinalityStateMachineConfiguration {

    public static final long CARDINALITY = 5L;

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

    public enum States {
        S1, S2, S3, S4, S5
    }

    public enum Events {
        E23, E34, E35
    }

    public final SpyAction a1 = new SpyAction();
    public final SpyAction a2 = new SpyAction();
    public final SpyAction a3 = new SpyAction();
    public final SpyAction a4 = new SpyAction();

    final Transition<States, Events> t1 = stp(States.S1, States.S2, a1);
    final Transition<States, Events> t2 = event(States.S2, States.S3, Events.E23, a2, () -> CARDINALITY);
    final Transition<States, Events> t3 = event(States.S3, States.S4, Events.E34, a3);
    final Transition<States, Events> t4 = event(States.S3, States.S5, Events.E35, a4);

    @Autowired
    MachineDefinitionBuilderProvider<States, Events> builderProvider;

    @Bean("TestCardinalityStateMachineDef")
    public MachineDefinition<States, Events> getMachineDefinition() {
        return builderProvider.getMachineDefinitionBuilder(States.class, Events.class)
                .name("toto")
                .states(States.values())
                .initialState(States.S1)
                .terminalStates(States.S4, States.S5)
                .events(Events.values())
                .transitions(t1, t2, t3, t4)
                .build();
    }
}
