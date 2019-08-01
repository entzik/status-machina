package com.thekirschners.statusmachina.handler.springjpa.repo;

import com.thekirschners.statusmachina.handler.springjpa.model.ExternalState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalStateRepository extends JpaRepository<ExternalState, String> {
}
