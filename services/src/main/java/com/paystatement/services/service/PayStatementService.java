package com.paystatement.services.service;

import com.paystatement.services.dto.GenerateStatementRequest;
import com.paystatement.services.model.*;
import com.paystatement.services.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@Service
public class PayStatementService {
    private static final BigDecimal WEEKLY_REGULAR_LIMIT = new BigDecimal("40");
    private static final BigDecimal OVERTIME_MULTIPLIER = new BigDecimal("1.5");
    private final EmployeeRepository employees;
    private final CompensationConfigRepository compensation;
    private final WeeklyHoursRepository weeklyHours;
    private final ModificationRequestRepository requests;
    private final WithholdingRuleRepository withholdingRules;
    private final DeductionRuleRepository deductionRules;
    private final PayCodeRuleRepository payCodeRules;
    private final PayStatementRepository statements;

    public PayStatementService(EmployeeRepository employees, CompensationConfigRepository compensation,
                               WeeklyHoursRepository weeklyHours, ModificationRequestRepository requests,
                               WithholdingRuleRepository withholdingRules, DeductionRuleRepository deductionRules,
                               PayCodeRuleRepository payCodeRules, PayStatementRepository statements) {
        this.employees = employees;
        this.compensation = compensation;
        this.weeklyHours = weeklyHours;
        this.requests = requests;
        this.withholdingRules = withholdingRules;
        this.deductionRules = deductionRules;
        this.payCodeRules = payCodeRules;
        this.statements = statements;
    }

    @Transactional
    public PayStatement generate(GenerateStatementRequest input) {
        Employee employee = employees.findById(input.employeeId())
                .filter(Employee::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Active employee not found"));
        BigDecimal rate = compensation.findById(employee.getEmployeeId())
                .map(CompensationConfig::getRegularHourlyRate)
                .orElseThrow(() -> new IllegalStateException("Hourly compensation is not configured for this employee"));
        BigDecimal hours = weeklyHours.findByEmployeeIdAndWeekEnding(employee.getEmployeeId(), input.weekEnding())
                .map(WeeklyHours::getHoursWorked)
                .orElseThrow(() -> new IllegalStateException("Weekly hours are not configured for this employee and week"));
        BigDecimal otherPaidHours = BigDecimal.ZERO;
        BigDecimal otherPay = BigDecimal.ZERO;
        List<ModificationRequest> approved = requests
                .findByEmployeeIdAndWeekEndingAndStatus(employee.getEmployeeId(), input.weekEnding(), "APPROVED");
        for (ModificationRequest adjustment : approved) {
            String payCode = adjustment.getPayCode().toUpperCase(Locale.ROOT);
            PayCodeRule payCodeRule = payCodeRules.findById(payCode)
                    .filter(PayCodeRule::isEnabled)
                    .orElseThrow(() -> new IllegalStateException("No active pay-code rule is configured for " + payCode));
            if (payCodeRule.isCountsTowardOvertime()) {
                if (payCodeRule.getMultiplier().compareTo(BigDecimal.ONE) != 0) {
                    throw new IllegalStateException("Overtime-eligible pay-code rules must use a 1.0 multiplier");
                }
                hours = hours.add(adjustment.getHours());
            } else {
                otherPaidHours = otherPaidHours.add(adjustment.getHours());
                otherPay = otherPay.add(adjustment.getHours().multiply(rate).multiply(payCodeRule.getMultiplier()));
            }
        }
        if (hours.add(otherPaidHours).compareTo(new BigDecimal("168")) > 0) {
            throw new IllegalStateException("Total paid hours cannot exceed 168 in a week");
        }
        BigDecimal regularHours = hours.min(WEEKLY_REGULAR_LIMIT);
        BigDecimal overtimeHours = hours.subtract(regularHours).max(BigDecimal.ZERO);
        BigDecimal gross = regularHours.multiply(rate)
                .add(overtimeHours.multiply(rate).multiply(OVERTIME_MULTIPLIER))
                .add(otherPay)
                .setScale(2, RoundingMode.HALF_UP);

        List<WithholdingRule> enabledTaxRules = withholdingRules.findByEnabledTrue();
        boolean hasFederalRule = enabledTaxRules.stream()
                .anyMatch(rule -> "FEDERAL".equalsIgnoreCase(rule.getScope()));
        boolean hasRegionalRule = enabledTaxRules.stream()
                .anyMatch(rule -> "REGION".equalsIgnoreCase(rule.getScope()) && employee.getRegion() == rule.getRegion());
        if (!hasFederalRule) {
            throw new IllegalStateException("An active federal tax rule must be configured before generating a statement");
        }
        if (!hasRegionalRule) {
            throw new IllegalStateException("An active tax rule must be configured for region " + employee.getRegion());
        }
        BigDecimal taxes = enabledTaxRules.stream()
                .filter(rule -> "FEDERAL".equalsIgnoreCase(rule.getScope())
                        || ("REGION".equalsIgnoreCase(rule.getScope()) && employee.getRegion() == rule.getRegion()))
                .map(rule -> calculate(rule.getKind(), rule.getValue(), gross))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deductions = deductionRules.findByEnabledTrue().stream()
                .map(rule -> calculate(rule.getKind(), rule.getValue(), gross))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        PayStatement statement = statements.findByEmployeeIdAndWeekEnding(employee.getEmployeeId(), input.weekEnding())
                .orElseGet(PayStatement::new);
        statement.setEmployeeId(employee.getEmployeeId());
        statement.setWeekEnding(input.weekEnding());
        statement.setRegularHours(regularHours);
        statement.setOvertimeHours(overtimeHours);
        statement.setOtherPaidHours(otherPaidHours);
        statement.setGrossPay(gross);
        statement.setTaxes(taxes);
        statement.setDeductions(deductions);
        statement.setNetPay(gross.subtract(taxes).subtract(deductions).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        statement.setStatus("GENERATED");
        return statements.save(statement);
    }

    private BigDecimal calculate(RuleKind kind, BigDecimal value, BigDecimal gross) {
        if (kind == RuleKind.PERCENTAGE) {
            if (value.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalStateException("Percentage rules must be between 0 and 100");
            }
            return gross.multiply(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public List<PayStatement> employeeStatements(String employeeId) {
        return statements.findByEmployeeIdOrderByWeekEndingDesc(employeeId);
    }
}
