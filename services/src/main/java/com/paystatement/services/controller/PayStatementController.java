package com.paystatement.services.controller;

import com.paystatement.services.dto.GenerateStatementRequest;
import com.paystatement.services.model.PayStatement;
import com.paystatement.services.repository.PayStatementRepository;
import com.paystatement.services.service.PayStatementGeneratedPublisher;
import com.paystatement.services.service.PayStatementService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/statements")
public class PayStatementController {
    private final PayStatementService service;
    private final PayStatementRepository statements;
    private final PayStatementGeneratedPublisher publisher;

    public PayStatementController(PayStatementService service, PayStatementRepository statements,
                                  PayStatementGeneratedPublisher publisher) {
        this.service = service;
        this.statements = statements;
        this.publisher = publisher;
    }

    @GetMapping
    public List<PayStatement> mine(Authentication authentication) {
        return service.employeeStatements(authentication.getName());
    }

    @GetMapping("/{id}")
    public PayStatement byId(@PathVariable Long id, Authentication authentication) {
        PayStatement statement = statements.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pay statement not found"));
        requireOwnerOrPrivileged(statement.getEmployeeId(), authentication);
        return statement;
    }

    @GetMapping("/employee/{employeeId}/status")
    public PayStatement status(@PathVariable String employeeId, @RequestParam LocalDate weekEnding,
                               Authentication authentication) {
        requireOwnerOrPrivileged(employeeId, authentication);
        return statements.findByEmployeeIdAndWeekEnding(employeeId, weekEnding)
                .orElseThrow(() -> new IllegalArgumentException("No pay statement exists for this employee and week"));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','PAYMENT_SPECIALIST','SYSTEM')")
    public PayStatement generate(@Valid @RequestBody GenerateStatementRequest request) {
        PayStatement statement = service.generate(request);
        publisher.publish(statement);
        return statement;
    }

    private void requireOwnerOrPrivileged(String employeeId, Authentication authentication) {
        boolean privileged = authentication.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PAYMENT_SPECIALIST")
                        || a.getAuthority().equals("ROLE_SYSTEM"));
        if (!privileged && !employeeId.equals(authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("Cannot access another employee's statement");
        }
    }
}
