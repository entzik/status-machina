package com.thekirschners.statusmachina.handler.springjpa;

import com.thekirschners.statusmachina.handler.StateMachineLockService;
import com.thekirschners.statusmachina.handler.springjpa.model.ExternalState;
import com.thekirschners.statusmachina.handler.springjpa.repo.ExternalStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service()
@Transactional
public class SpringJpaLockService implements StateMachineLockService {

    @Autowired
    ExternalStateRepository externalStateRepository;

    @Override
    public void lock(String id) {
        final ExternalState externalState = externalStateRepository.findById(id).orElseThrow();
        if (externalState.isLocked())
            throw new IllegalStateException("machine is locked by another instance, ID=" + id);
        else {
            externalState.setLocked(true);
            externalStateRepository.save(externalState);
        }
    }

    @Override
    public void release(String id) {
        final ExternalState externalState = externalStateRepository.findById(id).orElseThrow();
        if (!externalState.isLocked())
            throw new IllegalStateException("machine is not locked, ID=" + id);
        externalState.setLocked(false);
        externalStateRepository.save(externalState);
    }
}
