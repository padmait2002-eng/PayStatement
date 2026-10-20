package com.paystatement.services.controller;

import com.paystatement.services.model.DeductionRule;
import com.paystatement.services.model.WithholdingRule;
import com.paystatement.services.model.PayCodeRule;
import com.paystatement.services.repository.DeductionRuleRepository;
import com.paystatement.services.repository.PayCodeRuleRepository;
import com.paystatement.services.repository.WithholdingRuleRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/rules")
@PreAuthorize("hasAnyRole('ADMIN','PAYMENT_SPECIALIST')")
public class RuleAdminController {
    private final WithholdingRuleRepository withholding;
    private final DeductionRuleRepository deductions;
    private final PayCodeRuleRepository payCodes;

    public RuleAdminController(WithholdingRuleRepository withholding, DeductionRuleRepository deductions,
                               PayCodeRuleRepository payCodes) {
        this.withholding = withholding;
        this.deductions = deductions;
        this.payCodes = payCodes;
    }

    @GetMapping("/taxes")
    public List<WithholdingRule> taxes() { return withholding.findAll(); }

    @PostMapping("/taxes")
    public WithholdingRule saveTax(@Valid @RequestBody WithholdingRule rule) {
        validateRule(rule.getKind().name(), rule.getValue());
        if (!"FEDERAL".equals(rule.getScope()) && !"REGION".equals(rule.getScope())) {
            throw new IllegalArgumentException("Tax scope must be FEDERAL or REGION");
        }
        if ("REGION".equals(rule.getScope()) && rule.getRegion() == null) {
            throw new IllegalArgumentException("Regional tax rules require a region");
        }
        if ("FEDERAL".equals(rule.getScope())) rule.setRegion(null);
        return withholding.save(rule);
    }

    @GetMapping("/deductions")
    public List<DeductionRule> deductions() { return deductions.findAll(); }

    @PostMapping("/deductions")
    public DeductionRule saveDeduction(@Valid @RequestBody DeductionRule rule) {
        validateRule(rule.getKind().name(), rule.getValue());
        return deductions.save(rule);
    }

    @GetMapping("/pay-codes")
    public List<PayCodeRule> payCodes() { return payCodes.findAll(); }

    @PostMapping("/pay-codes")
    public PayCodeRule savePayCode(@Valid @RequestBody PayCodeRule rule) {
        rule.setPayCode(rule.getPayCode().trim().toUpperCase(java.util.Locale.ROOT));
        if (rule.isCountsTowardOvertime() && rule.getMultiplier().compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("Overtime-eligible pay-code rules must use a 1.0 multiplier");
        }
        return payCodes.save(rule);
    }

    private void validateRule(String kind, BigDecimal value) {
        if ("PERCENTAGE".equals(kind) && value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }
    }
}
