# Status Machina

__WORK IN PROGRESS__

A small, simple and pragmatic state machine engine targeted at resilient micro-services orchestration.

It offers a core library and a spring integration library.

The spring itegration libary persists and ensures distributed consensus on state machine transitions using a relational database and SpringDataJPA

## Usage
StatusMachina can be used on its own or in the context of a Spring Boot project. One step however remains the same is both cases, and that's defining the state machine.

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

you first need to define your transitions, then use the composable API to define the machine.

To define the machine you must define the initial state, one or more final states and also all the other states in between. Then you need to to specify all the events to which the state machine is expected to react, and of course, the transitions triggered by these events out of various states. In total 8 lines of composable API.

There are some rules though. Building the machine definition will fail if
1. a state that is not final does not have any transition being defined out of it
2. if transitions are defined out of a final state

The code snippet bellow shows how to create a state machine definition:

```java
import io.statusmachina.core.api.MachineDefinition;
import io.statusmachina.core.api.Transition;
import io.statusmachina.core.stdimpl.EnumBasedMachineDefinitionBuilderProvider;

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

A transition takes the machine from a state to another. You probably already noticed by looking at the code snippet above that transitions can be of two kinds: event triggered and STP.

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

If the post transition action fails, the machine will enter an error state with sufficient metadata to describe the error condition and context.

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

If such transitent data needs to be passed from an action to the subsequent post action, the stashing mechanism can be used. This will allow an action to stash any kind of object which can then be retrieved (un-stashed) by the post action, like in the example bellow:

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

Please remember the stash only exists during the lifecycle of the transition where it was defined. Once the transition completes, it will be discarded.

#### Transition Guards

Transitions can be guarded. A Guard is a predicate that consumes the machine's current context and must evaluate to true in order for the transition to activate. It can be passed to any transition as a lambda:

```java
final Transition<States, Events> t3 = stp(States.S3, States.S4, context -> "some-value".equals(context.get("context-key")));
```

It is best to think abbout a guard as a pre-condition of the transition.


### Instantiating a State Machine

Once the state machine is defined you can create an instance and start interacting with it.

To instantiate a state machine you need to provide a definition, a set of callbacks that will help you manage the machine's lifecycle and the initial context.

The callbacks will allow you to react when a machine needs to be saved to persistent storage. You should not have to implement these specializations by yourself, unless you are writing an integration layer. Specializations of the state machine, such as the spring boot integration, are expected to provide implementations of these callbacks.

The context is the initial data you inject in the machine. The context can be read by transition guards, transition actions and post transition actions and it can be modified by transition actions.

```java
final Machine<States, Events> instance = new MachineInstanceImpl<>(def, machinePersistenceCallback, new HashMap<>())
                                            .start();
```

Keep in mind you need to explicitely start the state machine after instantiating it. If you have STP transitions out of the initial state, starting the machine will evaluate evaluated each STP transition's guard and fire the first one that matches. 

Pro tip: in order for your machine to be deterministic, you need to make sure guarding conditions on your transitions defined out of the same state are mutually exclusive.

Interacting with a state machine is as easy as sending an event to it:

```java
final Machine<States, Events> updated = instance.sendEvent(Events.E23);
```

The state machine is an immutable data structure. Any atempt to mutate its internal state will create a new version of the machine. As such, sending an event to a machine instance will create a new instance that will reflect the new state. 

When sending an event to the machine you can also specify a custom parameter. This parameter will be consumed by the associated transition action and post transition action. The event parameter can be of any type and is purely transient, the machine will never hold on to it.

```java
final Machine<States, Events> updated = instance.sendEvent(Events.E23, someParameter);
```

Pro tip: whenever a machine transition to a new state, whether following the delivery of an event or through STP, as soon as it gets into the new state all STP transitions out of that state will be evaluated and possibly triggered. The machine will so transition from state to state until there are no more STP transitions defined out of the current state or none of the guard predicates evaluate.

# SpringBoot Integration

__TODO__
