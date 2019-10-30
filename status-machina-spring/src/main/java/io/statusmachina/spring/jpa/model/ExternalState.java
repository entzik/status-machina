/*
 *
 * Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package io.statusmachina.spring.jpa.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Map;

@Entity
@Table(name = "sm_states", indexes = {
        @Index(columnList = "done")
})
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

    @Column(name = "error")
    String error;

    @Column(name = "done")
    boolean done;

    @Column(name = "locked")
    @NotNull
    boolean locked;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    @CollectionTable(name = "sm_context_entries", joinColumns = @JoinColumn(name = "machine_id"))
    Map<String, String> context;

    /**
     * milliseconds since last time this state was updated
     */
    @Column(name = "last_modified")
    @NotNull
    long lastModifiedEpoch;

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

    public long getLastModifiedEpoch() {
        return lastModifiedEpoch;
    }

    public ExternalState setLastModifiedEpoch(long lastModifiedEpoch) {
        this.lastModifiedEpoch = lastModifiedEpoch;
        return this;
    }

    public String getError() {
        return error;
    }

    public ExternalState setError(String error) {
        this.error = error;
        return this;
    }

    public ExternalState setVersion(int version) {
        this.version = version;
        return this;
    }

    public boolean isDone() {
        return done;
    }

    public ExternalState setDone(boolean done) {
        this.done = done;
        return this;
    }
}
