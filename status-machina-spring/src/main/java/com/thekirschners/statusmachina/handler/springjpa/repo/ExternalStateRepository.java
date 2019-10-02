package com.thekirschners.statusmachina.handler.springjpa.repo;

import com.thekirschners.statusmachina.handler.springjpa.model.ExternalState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExternalStateRepository extends JpaRepository<ExternalState, String> {
    List<ExternalState> findAllByLastModifiedEpochLessThan(long lastModifiedEpoch);
    List<ExternalState> findAllByCurrentState(String currentSteter);
}
