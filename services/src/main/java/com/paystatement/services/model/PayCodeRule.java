package com.paystatement.services.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "pay_code_rules")
public class PayCodeRule {
    @Id
    @NotBlank
    @Column(length = 32)
    private String payCode;
    @NotBlank
    private String description;
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal multiplier;
    @Column(nullable = false)
    private boolean countsTowardOvertime;
    @Column(nullable = false)
    private boolean enabled = true;

    public String getPayCode() { return payCode; }
    public void setPayCode(String payCode) { this.payCode = payCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getMultiplier() { return multiplier; }
    public void setMultiplier(BigDecimal multiplier) { this.multiplier = multiplier; }
    public boolean isCountsTowardOvertime() { return countsTowardOvertime; }
    public void setCountsTowardOvertime(boolean countsTowardOvertime) { this.countsTowardOvertime = countsTowardOvertime; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
