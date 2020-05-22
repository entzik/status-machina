# Status Machina

__WORK IN PROGRESS__

A small, simple and pragmatic state machine engine targeted at resilient micro-services orchestration.

It offers a core library and a spring integration library.

The spring itegration libary persists and ensures distributed consensus on state machine transitions using a relational database and SpringDataJPA

## Usage
Status machina can be used on its own or in the context of a Spring Boot project. One step however remains the same is both cases, and that's defining the state machine.

### Defining a state machine

A state machine is defined in terms of states and transitions. Transitions are triggered by events. At the lowest level, states and events can be of any type, but it's most covenient if they are enumerations. 

Defining an enumeration based state machine is very easy. Given the machines states and evente enumerations

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

An action can be executed each time a the machine transitions from one state to another. This allows the state machine to interact with external services. This can be a remote procedure call, posting a message on a message bus or anything else. If executing the action fails, the machine will enter an error state.

A transition action consumes the machine's context and the event's parameter, if one was provided, and produces a new context which will become the machine's context if the action succeeds.

```java

```

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


