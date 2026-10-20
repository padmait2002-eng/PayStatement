package com.paystatement.services.repository;

import com.paystatement.services.model.PayCodeRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayCodeRuleRepository extends JpaRepository<PayCodeRule, String> {}
