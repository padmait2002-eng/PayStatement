package com.paystatement.services.repository;

import com.paystatement.services.model.CompensationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompensationConfigRepository extends JpaRepository<CompensationConfig, String> {}
