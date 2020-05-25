# Status Machina

__WORK IN PROGRESS__

A small, simple and pragmatic state machine engine targeted at resilient micro-services orchestration.

It offers a core library and a spring integration library.

The spring itegration libary persists and ensures distributed consensus on state machine transitions using a relational database and SpringDataJPA

## Usage
Status machina can be used on its own or in the context of a Spring Boot project. One step however remains the same is both cases, and that's defining the state machine.

### Defining a state machine

A state machine is defined in terms of states and transitions. Transitions are triggered by events. At the lowest level, states and events can be of any type, but it's most covenient if they are enumerations. 

Defining an enumeration based state machine is very easy. Given the machine's states and event enumerations

```java
enum States {
    S1, S2, S3, S4, S5
}

enum Events {
    E23, E34, E35
}
```

you first need to define your transitions, then use the composable API to define the machine:

```java
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.Transition;
import io.statusmachina.core.stdimpl.EnumBasedMachineDefinitionBuilderProvider;
import org.junit.jupiter.api.Test;

import static io.statusmachina.core.api.Transition.*;

final Transition<States, Events> t1 = stp(States.S1, States.S2);
final Transition<States, Events> t2 = event(States.S2, States.S3, Events.E23);
final Transition<States, Events> t3 = event(States.S3, States.S4, Events.E34);
final Transition<States, Events> t4 = event(States.S3, States.S5, Events.E35);

final MachineDefinition<States, Events> def = new EnumBasedMachineDefinitionBuilderProvider().getMachineDefinitionBuilder(States.class, Events.class)
        .name("toto")
        .states(States.values())
        .initialState(States.S1)
        .terminalStates(States.S4, States.S5)
        .events(Events.values())
        .transitions(t1, t2, t3, t4)
        .build();

```

### Transitions

A transition takes the machine from a state to another. You probably already noticed by looking at the code that they can be of two kinds: event transitions and STP transitions.

Event transitions are triggerd when an appropriate event is delivered to the machine. If a machine is in a state and a transition is configured our of that state for a specific event, then when that event is delivered, the machine will immediately transition to the target state of the transition.

STP stands for Straight Through Processing. STP transitions are triggered automatically: when a machine reaches a state, and an STP transition is configured out of that state, the machine will immediately transition to the target state of the STP transition.

#### Transition Actions

An action can be executed each time a the machine transitions from one state to another. This allows the state machine to interact with external services. This can be a remote procedure call, posting a message on a message bus or anything else. If executing the action fails, the machine will enter an error state with sufficient metadata to describe the error condition and context.

A transition action consumes the machine's context and the event's parameter, if one was provided, and produces a new context which will become the machine's context if the action succeeds.

```java
class SomeTransitionAction implement TransitionAction {
    @Override
    public ImmutableMap<String, String> apply(ImmutableMap context, Object eventParameter) {
        // perform an action, modify context
        return modifiedContext;
    }
};
```

You notice the context is an immutable map, so if you need to modify it you need to clone it.

#### Post Transition Actions

It is also possible to configure a post transition action. The post transition action, if one is specified, is only invoked after the transition has completed and the machine has durably reached the target state.

If the post transition action fails, the machine will not reach the target state, enter an error state with sufficient metadata to describe the error condition and context.

A post transition action is not allowed to mutate the machine's context, it can only consume it.

```java
static class SomePostTransitionAction<P> implements TransitionPostAction<P> {
    @Override
    public void accept(ImmutableMap<String, String> context, P p) {
        // do something with context and event parameters
    }
}
```

Post transition actions are typically used to implement notifications related to the new state.

#### Passing Information Between Transition Actions and Post Transition Actions

Sometimes we may need to pass information between actions and post actions. This can of course be done through the context - because the post transition action will receive the context mutated by the action, but if the information is not needed later in the process it will just polute the context.

If such transitent data needs to be passed from an action to the subsequent post action, the stashing mechanism can be used. This will allow an action to stash any object which can then be retrieved (un-stashed) by the post action, like in the example bellow:

```java
class SomeTransitionAction implement TransitionAction {
    @Override
    public ImmutableMap<String, String> apply(ImmutableMap context, Object eventParameter) {
        // perform an action, modify context
        stash("my-key", "my object");
        return modifiedContext;
    }
};

static class SomePostTransitionAction<P> implements TransitionPostAction<P> {
    @Override
    public void accept(ImmutableMap<String, String> context, P p) {
        // do something with context and event parameters
        String stashedValue = getStash("my-key", String.class);
    }
}
```

Please remember the stash only exists during the lifecycle of the transition where it was defined.

#### Transition Guards

Transitions can be guarded. A Guard is a predicate that consumes the machine's current context and must evaluate to true in order for the transition to activate. It can be passed to any transition as a lambda:

```java
final Transition<States, Events> t3 = stp(States.S3, States.S4, context -> "some-value".equals(context.get("context-key")));
```


### Creating a state machine and interracting with it

Once the state machine is defined you can create an instance and start interacting with it.

To instantiate a state machine you need to provide a definition, a set of callbacks that will help you manage the machine's lifecycle and the initial context.

The callbacks will allow you to react when a machine needs to be saved to persistent storage.

The context is the initial data you inject in the machine. The context can be read and moified by transition actions.

```java
final Machine<States, Events> instance = new MachineInstanceImpl<>(def, machinePersistenceCallback, new HashMap<>()).start();
```


