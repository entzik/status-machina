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

The StatusMachina SpringBoot integration defines its own spring boot starter. It persists a state machine data in the current Spring Boot data source and provides a companion spring service for convenience. Apart from making sure the state machine tables and indexes are defines in the database pointed at by the data source, there is nothing much to do.

## Setting up the database

The recomended way to set up the database is using Liquibase change sets, but of course that is ultimately up to you. 
The code snippet bellow shows a Liquibase change set for create the required database table.

```xml
    <changeSet id="3" author="Emil Kirschner">
        <createTable tableName="sm_context_entries">
            <column name="machine_id" type="varchar(40)"></column>
            <column name="name" type="varchar(255)"></column>
            <column name="value" type="varchar(255)"></column>
        </createTable>
        <addPrimaryKey columnNames="machine_id, name"
                       constraintName="pk_sm_context_entries"
                       tableName="sm_context_entries" />

        <createTable tableName="sm_states">
            <column name="id" type="varchar(40)">
                <constraints primaryKey="true" primaryKeyName="statemachine_pk" nullable="false"/>
            </column>
            <column name="version" type="bigint"></column>
            <column name="crt_state" type="varchar(255)"></column>
            <column name="done" type="boolean"></column>
            <column name="error" type="varchar(255)"></column>
            <column name="last_modified" type="bigint"></column>
            <column name="locked" type="boolean"></column>
            <column name="typename" type="varchar(255)"></column>
        </createTable>
    </changeSet>
```

## Defining a state machine in Spring Boot

You can define an enum based state machine in Spring Boot almost the same way we've seen it above, but since this is Spring you may want to expose the definition as a Spring Bean using a Spring Configuration.

You need to autowire a ```MachineDefinitionBuilderProvider``` instance which will give you a ready to use ```MachineDefinitionBuilder``` instance.

It is good practice to name your ```MachineDefinitionBuilder``` beans in order to avoid conflicts of you define more than one state machine.

```java
...

@Configuration
public class TestOneStateMachineConfiguration {

    ...
    
    public enum States {
        S1, S2, S3, S4, S5
    }

    public enum Events {
        E23, E34, E35
    }

    ...
    
    final Transition<States, Events> t1 = stp(States.S1, States.S2);
    final Transition<States, Events> t2 = event(States.S2, States.S3, Events.E23);
    final Transition<States, Events> t3 = event(States.S3, States.S4, Events.E34);
    final Transition<States, Events> t4 = event(States.S3, States.S5, Events.E35);

    @Autowired
    MachineDefinitionBuilderProvider<States, Events> builderProvider;

    @Bean("MyMachineDef")
    public MachineDefinition<States, Events> getMachineDefinition() {
        return builderProvider.getMachineDefinitionBuilder(States.class, Events.class)
                .name("myMachine")
                .states(States.values())
                .initialState(States.S1)
                .terminalStates(States.S4, States.S5)
                .events(Events.values())
                .transitions(t1, t2, t3, t4)
                .build();
    }
}
```

## Instantiating and using a state machine in Spring Boot

Now that you have your state machine definition, all you have to do is autowire it and use the companion spring service to instantiate and persist the machine instance in its initial state (or any state STP transition may have taken it from there).

The code snippet bellow shows how easy it is to interact with the machine in SpringBoot:

```java
    ...
    
    @Autowired
    @Qualifier("MyMachineDef") // autowire the machine definition using the qualifier to avoid conflicts
    MachineDefinition<States, Events> def;

    @Autowired // autowire the state machine companion service
    StateMachineService<TestOneStateMachineConfiguration.States, TestOneStateMachineConfiguration.Events> service;

    public void someService() {
        // instantiate and start a new machine using the provided definition
        final Machine machine = service.newMachine(def, new HashMap<>()).start();
        // you can get the machine ID for future reference
        final String machineId = machine.getId();
        // you can send an event to the machine. the machine will persist itself to the database.
        machine.sendEvent(Events.E23);    
    }

```

As you notice, you don't have to care about persistence. The state machine instance configured by the ```StateMachineService``` will automatically persist itself each time the state changes.


The ```StateMachineService``` offers a set of useful functionalities that help manage a fleet of state machine instances. You can use these functions to provide operational monitoring and remediation actions in your application. You can:

- find all machines that have terminated
- find all machines that are in an error state, and get details about those errors
- find all machines that are stalled, that is, they are not in a terminal state and have not received an event in a specified amoutn of time.
- take a machine out of error state and have it resume its function.

The code snippet bellow shows typical usage examples of this functionality.

```java
    // you can read a state machine from the database
    final Machine<States, Events> instance = service.read(def, machineId);   
    
    // you can find all terminated machines
    final List<MachineSnapshot> terminated = service.findTerminated();
    
    
    // you can find stale machines. a machine is stale if it hasn't received an event in a specified number of seconds
    final List<MachineSnapshot> stale = service.findStale(1);
    
    // you can find machines in failed state
    final List<MachineSnapshot> failed = service.findFailed();
    
    // you can get detailed information about the failure the machine suffered:
    instance.getErrorType(); // get the error type
    instance.getError(); // get the error message, if any
```

## Transactional Considerations

The state machine Spring Boot integration implements its own transaction boundary policies. You need to be aware of this behavior when integrating Status Machina in your project.

- When a machine is instantiated, it is imediately persisted in its initial state. The persistence code is wrapped in a transaction template.
- Each time the machine transitions to a new state, the transition logic and the transition action are executed as part of the same transaction. If the transiton action fails with an exception, the transaction will roll back and the state change will not happen. If the state machine itself fails to persist, the transaction will also be rolled back, the machine will not change states and whatever database operation the action has performed during the transition transaction, will of course be rolled back as well.

The transaction propagation policy used by the Spring Boot integration defaults to ```PROPAGATION_REQUIRED_NEW```. This works well for the projects StatusMachina was initialy developed to serve, but you can change it to ```PROPAGATION_REQUIRED``` if that works better for you.

To change the transaction propagation mode you must set the ```statusmachina.spring.transactionPropagation``` property to the desired value. This is expected to be a numeric value, as defined in the ```org.springframework.transaction.TransactionDefinition``` interface.

The transaction isolation level defaults to ```ISOLATION_DEFAULT```. You can change it using the ```statusmachina.spring.transactionIsolation``` property using numeric values defined in the ```org.springframework.transaction.TransactionDefinition``` interface, but I strongly recommend you only do that only if you know very well what your doing.
