package com.paystatement.services.controller;

import com.paystatement.services.dto.ModificationRequestInput;
import com.paystatement.services.model.Employee;
import com.paystatement.services.model.ModificationRequest;
import com.paystatement.services.repository.EmployeeRepository;
import com.paystatement.services.repository.ModificationRequestRepository;
import com.paystatement.services.repository.WeeklyHoursRepository;
import com.paystatement.services.service.PayStatementGeneratedPublisher;
import com.paystatement.services.service.PayStatementService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@RestController
@RequestMapping("/api")
public class EmployeeController {
    private final EmployeeRepository employees;
    private final ModificationRequestRepository requests;
    private final WeeklyHoursRepository weeklyHours;
    private final PayStatementService statements;
    private final PayStatementGeneratedPublisher publisher;

    public EmployeeController(EmployeeRepository employees, ModificationRequestRepository requests,
                              WeeklyHoursRepository weeklyHours, PayStatementService statements,
                              PayStatementGeneratedPublisher publisher) {
        this.employees = employees;
        this.requests = requests;
        this.weeklyHours = weeklyHours;
        this.statements = statements;
        this.publisher = publisher;
    }

    @GetMapping("/me")
    public Employee me(Authentication authentication) {
        return employees.findById(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("No employee record exists for this identity"));
    }

    @PostMapping("/requests")
    public ModificationRequest submit(@Valid @RequestBody ModificationRequestInput input,
                                      Authentication authentication) {
        ModificationRequest request = new ModificationRequest();
        request.setEmployeeId(authentication.getName());
        request.setWeekEnding(input.weekEnding());
        request.setHours(input.hours());
        request.setPayCode(input.payCode().trim());
        request.setExplanation(input.explanation().trim());
        return requests.save(request);
    }

    @GetMapping("/requests/mine")
    public List<ModificationRequest> myRequests(Authentication authentication) {
        return requests.findByEmployeeIdOrderBySubmittedAtDesc(authentication.getName());
    }

    @GetMapping("/specialist/requests")
    @PreAuthorize("hasAnyRole('ADMIN','PAYMENT_SPECIALIST')")
    public List<ModificationRequest> pendingRequests() {
        return requests.findAllByOrderBySubmittedAtDesc();
    }

    @PostMapping("/specialist/requests/{id}/decision")
    @PreAuthorize("hasAnyRole('ADMIN','PAYMENT_SPECIALIST')")
    @Transactional
    public ModificationRequest decide(@PathVariable Long id, @RequestParam boolean approve,
                                      Authentication authentication) {
        ModificationRequest request = requests.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Modification request not found"));
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Only pending requests can be reviewed");
        }
        request.setStatus(approve ? "APPROVED" : "REJECTED");
        request.setReviewedBy(authentication.getName());
        ModificationRequest reviewed = requests.save(request);
        if (approve && weeklyHours.findByEmployeeIdAndWeekEnding(request.getEmployeeId(), request.getWeekEnding()).isPresent()) {
            var statement = statements.generate(new com.paystatement.services.dto.GenerateStatementRequest(
                    request.getEmployeeId(), request.getWeekEnding()));
            publisher.publishAfterCommit(statement);
        }
        return reviewed;
    }
}
