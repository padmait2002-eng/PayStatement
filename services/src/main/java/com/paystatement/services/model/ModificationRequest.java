package com.paystatement.services.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "modification_requests")
public class ModificationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String employeeId;
    @NotNull
    private LocalDate weekEnding;
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal hours;
    @NotBlank
    private String payCode;
    @NotBlank
    private String explanation;
    private String status = "PENDING";
    private String reviewedBy;
    private Instant submittedAt = Instant.now();

    public Long getId() { return id; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public LocalDate getWeekEnding() { return weekEnding; }
    public void setWeekEnding(LocalDate weekEnding) { this.weekEnding = weekEnding; }
    public BigDecimal getHours() { return hours; }
    public void setHours(BigDecimal hours) { this.hours = hours; }
    public String getPayCode() { return payCode; }
    public void setPayCode(String payCode) { this.payCode = payCode; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public Instant getSubmittedAt() { return submittedAt; }
}
