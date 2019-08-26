package com.thekirschners.statusmachina.handler.springjpa.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Map;

@Entity
@Table(name = "state_machine_states")
public class ExternalState {
    @Id
    @Column(
            name = "id",
            updatable = false,
            nullable = false,
            length = 64
    )
    String id;

    @Version
    @Column(name = "version")
    int version;

    @Column(name = "typename")
    @NotNull
    String type;

    @Column(name = "crt_state")
    @NotNull
    String currentState;

    @Column(name = "locked")
    @NotNull
    boolean locked;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name="name")
    @Column(name="value")
    @CollectionTable(name="example_attributes", joinColumns=@JoinColumn(name="machine_id"))
    Map<String,String> context;

    public ExternalState() {
    }

    public String getId() {
        return id;
    }

    public ExternalState setId(String id) {
        this.id = id;
        return this;
    }

    public long getVersion() {
        return version;
    }

    public boolean isLocked() {
        return locked;
    }

    public ExternalState setLocked(boolean locked) {
        this.locked = locked;
        return this;
    }

    public String getType() {
        return type;
    }

    public ExternalState setType(String type) {
        this.type = type;
        return this;
    }

    public String getCurrentState() {
        return currentState;
    }

    public ExternalState setCurrentState(String currentState) {
        this.currentState = currentState;
        return this;
    }

    public Map<String, String> getContext() {
        return context;
    }

    public ExternalState setContext(Map<String, String> context) {
        this.context = context;
        return this;
    }
}
