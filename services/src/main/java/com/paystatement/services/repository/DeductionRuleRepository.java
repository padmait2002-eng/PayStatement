package com.paystatement.services.repository;

import com.paystatement.services.model.DeductionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeductionRuleRepository extends JpaRepository<DeductionRule, Long> {
    List<DeductionRule> findByEnabledTrue();
}
