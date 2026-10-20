package com.paystatement.services.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PayStatementGeneratedEvent(Long statementId, String employeeId, LocalDate weekEnding,
                                         String status, Instant generatedAt) {}
