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

package io.statusmachina.spring.jpa;

import io.statusmachina.core.spi.StateMachineLockService;
import io.statusmachina.spring.jpa.model.ExternalState;
import io.statusmachina.spring.jpa.repo.ExternalStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service()
@Transactional
public class SpringJpaLockService implements StateMachineLockService {

    @Autowired
    ExternalStateRepository externalStateRepository;

    @Override
    public boolean lock(String id) {
        final ExternalState externalState = externalStateRepository.findById(id).orElseThrow();
        if (externalState.isLocked())
            throw new IllegalStateException("machine is locked by another instance, ID=" + id);
        else {
            externalState.setLocked(true);
            externalStateRepository.save(externalState);
            return true;
        }
    }

    @Override
    public boolean release(String id) {
        final ExternalState externalState = externalStateRepository.findById(id).orElseThrow();
        if (!externalState.isLocked())
            throw new IllegalStateException("machine is not locked, ID=" + id);
        else {
            externalState.setLocked(false);
            externalStateRepository.save(externalState);
            return true;
        }
    }
}
