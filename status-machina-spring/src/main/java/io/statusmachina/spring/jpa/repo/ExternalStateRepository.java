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

package io.statusmachina.spring.jpa.repo;

import io.statusmachina.spring.jpa.model.ExternalState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExternalStateRepository extends JpaRepository<ExternalState, String> {
    @Query("select s from ExternalState s where s.errorType = 'NONE' and s.done is false and s.lastModifiedEpoch < ?1")
    List<ExternalState> findAllByLastModifiedEpochLessThan(long lastModifiedEpoch);
    List<ExternalState> findAllByCurrentState(String currentState);
    List<ExternalState> findAllByDone(boolean done);
}
