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

package io.statusmachina.core.api;

import com.google.common.collect.ImmutableMap;

import java.util.function.Predicate;

/**
 * A transition guard determines if the transition it is attached to can be activated or not. The decision must be taken
 * upon examining the machine's context
 */
public interface TransitionGuard extends Predicate<ImmutableMap<String, String>> {
    /**
     * decide whether the transition can be executed
     * @param context the state machine's context
     * @return true of the machine can be activated
     */
    @Override
    boolean test(ImmutableMap<String, String> context);
}
