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

package io.statusmachina.core.api;

import java.util.Map;

/**
 * an external representation of the state machine, returned by management and reporting services
 */
public class MachineSnapshot {
    final String type;
    final String id;
    final String crtState;
    final Map<String,String> context;
    private ErrorType errorType;
    final String error;

    /**
     * construct an internal representation of the state machine
     *  @param type the name of the {@link MachineDefinition}
     * @param id the id of the {@link Machine}
     * @param crtState the current state
     * @param context the context
     * @param errorType
     * @param error error description, if applicable
     */
    public MachineSnapshot(String type, String id, String crtState, Map<String, String> context, ErrorType errorType, String error) {
        this.type = type;
        this.id = id;
        this.crtState = crtState;
        this.context = context;
        this.errorType = errorType;
        this.error = error;
    }

    /**
     * the name of the {@link MachineDefinition}
     */
    public String getType() {
        return type;
    }

    /**
     * the id of the {@link Machine}
     */
    public String getId() {
        return id;
    }

    /**
     * the context of the state machine
     */
    public Map<String, String> getContext() {
        return context;
    }

    /**
     * @return the type of error, if any
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * the error message
     */
    public String getError() {
        return error;
    }
}
