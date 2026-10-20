package com.paystatement.services.controller;

import com.paystatement.services.model.*;
import com.paystatement.services.repository.*;
import com.paystatement.services.repository.PayStatementRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','PAYMENT_SPECIALIST')")
public class EmployeeAdminController {
    private final EmployeeRepository employees;
    private final CompensationConfigRepository compensation;
    private final WeeklyHoursRepository hours;
    private final PayStatementRepository statements;

    public EmployeeAdminController(EmployeeRepository employees, CompensationConfigRepository compensation,
                                   WeeklyHoursRepository hours,
                                   PayStatementRepository statements) {
        this.employees = employees;
        this.compensation = compensation;
        this.hours = hours;
        this.statements = statements;
    }

    @GetMapping("/employees")
    public List<Employee> employees() { return employees.findAll(); }

    @PostMapping("/employees")
    public Employee saveEmployee(@Valid @RequestBody Employee employee) {
        return employees.save(employee);
    }

    @PutMapping("/employees/{employeeId}/compensation")
    public CompensationConfig saveCompensation(@PathVariable String employeeId,
                                                @Valid @RequestBody CompensationConfig config) {
        if (!employees.existsById(employeeId)) throw new IllegalArgumentException("Employee not found");
        config.setEmployeeId(employeeId);
        return compensation.save(config);
    }

    @GetMapping("/weekly-hours")
    public List<WeeklyHours> weeklyHours() { return hours.findAll(); }

    @PostMapping("/weekly-hours")
    public WeeklyHours saveWeeklyHours(@Valid @RequestBody WeeklyHours weeklyHours) {
        if (!employees.existsById(weeklyHours.getEmployeeId())) throw new IllegalArgumentException("Employee not found");
        return hours.findByEmployeeIdAndWeekEnding(weeklyHours.getEmployeeId(), weeklyHours.getWeekEnding())
                .map(existing -> {
                    existing.setHoursWorked(weeklyHours.getHoursWorked());
                    return hours.save(existing);
                }).orElseGet(() -> hours.save(weeklyHours));
    }

    @GetMapping("/regions")
    public Region[] regions() { return Region.values(); }

    @GetMapping("/statements")
    public List<PayStatement> statements(@RequestParam String employeeId) {
        if (!employees.existsById(employeeId)) throw new IllegalArgumentException("Employee not found");
        return statements.findByEmployeeIdOrderByWeekEndingDesc(employeeId);
    }
}
