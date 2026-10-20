package com.paystatement.services.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "weekly_hours", uniqueConstraints = @UniqueConstraint(columnNames = {"employeeId", "weekEnding"}))
public class WeeklyHours {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private String employeeId;
    @NotNull
    private LocalDate weekEnding;
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("168.00")
    private BigDecimal hoursWorked;

    public Long getId() { return id; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public LocalDate getWeekEnding() { return weekEnding; }
    public void setWeekEnding(LocalDate weekEnding) { this.weekEnding = weekEnding; }
    public BigDecimal getHoursWorked() { return hoursWorked; }
    public void setHoursWorked(BigDecimal hoursWorked) { this.hoursWorked = hoursWorked; }
}
