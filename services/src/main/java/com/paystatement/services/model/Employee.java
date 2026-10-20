package com.paystatement.services.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @NotBlank
    private String employeeId;
    @NotBlank
    private String name;
    @Enumerated(EnumType.STRING)
    @NotNull
    private Region region;
    @DecimalMin("0.00")
    private BigDecimal incentives = BigDecimal.ZERO;
    @Column(nullable = false)
    private boolean active = true;

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public BigDecimal getIncentives() { return incentives; }
    public void setIncentives(BigDecimal incentives) { this.incentives = incentives; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
