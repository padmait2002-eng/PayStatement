package com.paystatement.services.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

@Entity
@Table(name = "compensation_configs")
public class CompensationConfig {
    @Id
    private String employeeId;
    @DecimalMin("0.01")
    private BigDecimal regularHourlyRate;

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public BigDecimal getRegularHourlyRate() { return regularHourlyRate; }
    public void setRegularHourlyRate(BigDecimal regularHourlyRate) { this.regularHourlyRate = regularHourlyRate; }
}
