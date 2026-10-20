package com.paystatement.services.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "deduction_rules")
public class DeductionRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String name;
    @Enumerated(EnumType.STRING)
    @NotNull
    private RuleKind kind;
    @NotNull
    @DecimalMin("0.00")
    @Column(name = "rule_value", nullable = false)
    private BigDecimal value;
    @Column(nullable = false)
    private boolean enabled = true;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public RuleKind getKind() { return kind; }
    public void setKind(RuleKind kind) { this.kind = kind; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
