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

package io.statusmachina.core.stdimpl;

import com.google.common.collect.ImmutableMap;
import io.statusmachina.core.api.Machine;

public class MachineAndStash<S,E> {
    final private Machine<S, E> machineInstance;
    final private ImmutableMap<String, Object> stashStore;

    public MachineAndStash(Machine<S, E> machineInstance, ImmutableMap<String, Object> stashStore) {
        this.machineInstance = machineInstance;
        this.stashStore = stashStore;
    }

    public Machine<S, E> getMachine() {
        return machineInstance;
    }

    public ImmutableMap<String, Object> getStashStore() {
        return stashStore;
    }
}
