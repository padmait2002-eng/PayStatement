package com.paystatement.services.repository;

import com.paystatement.services.model.WithholdingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WithholdingRuleRepository extends JpaRepository<WithholdingRule, Long> {
    List<WithholdingRule> findByEnabledTrue();
}
