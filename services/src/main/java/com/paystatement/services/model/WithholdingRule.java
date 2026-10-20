package com.paystatement.services.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "withholding_rules")
public class WithholdingRule {
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
    private String scope;
    @Enumerated(EnumType.STRING)
    private Region region;
    @Column(nullable = false)
    private boolean enabled = true;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public RuleKind getKind() { return kind; }
    public void setKind(RuleKind kind) { this.kind = kind; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
