package com.paystatement.services.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GenerateStatementRequest(@NotBlank String employeeId, @NotNull LocalDate weekEnding) {}
