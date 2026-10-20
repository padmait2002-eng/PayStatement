package com.paystatement.services.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ModificationRequestInput(@NotNull LocalDate weekEnding,
                                       @NotNull @DecimalMin("0.01") @DecimalMax("168.00") BigDecimal hours,
                                       @NotBlank String payCode,
                                       @NotBlank String explanation) {}
