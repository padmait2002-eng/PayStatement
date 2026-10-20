package com.paystatement.services.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "pay_statements", uniqueConstraints = @UniqueConstraint(columnNames = {"employeeId", "weekEnding"}))
public class PayStatement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String employeeId;
    private LocalDate weekEnding;
    private BigDecimal regularHours;
    private BigDecimal overtimeHours;
    private BigDecimal otherPaidHours;
    private BigDecimal grossPay;
    private BigDecimal taxes;
    private BigDecimal deductions;
    private BigDecimal netPay;
    private String status = "GENERATED";
    private Instant generatedAt = Instant.now();

    public Long getId() { return id; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public LocalDate getWeekEnding() { return weekEnding; }
    public void setWeekEnding(LocalDate weekEnding) { this.weekEnding = weekEnding; }
    public BigDecimal getRegularHours() { return regularHours; }
    public void setRegularHours(BigDecimal regularHours) { this.regularHours = regularHours; }
    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }
    public BigDecimal getOtherPaidHours() { return otherPaidHours; }
    public void setOtherPaidHours(BigDecimal otherPaidHours) { this.otherPaidHours = otherPaidHours; }
    public BigDecimal getGrossPay() { return grossPay; }
    public void setGrossPay(BigDecimal grossPay) { this.grossPay = grossPay; }
    public BigDecimal getTaxes() { return taxes; }
    public void setTaxes(BigDecimal taxes) { this.taxes = taxes; }
    public BigDecimal getDeductions() { return deductions; }
    public void setDeductions(BigDecimal deductions) { this.deductions = deductions; }
    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getGeneratedAt() { return generatedAt; }
}
