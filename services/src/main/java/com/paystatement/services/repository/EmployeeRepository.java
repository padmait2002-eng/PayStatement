package com.paystatement.services.repository;

import com.paystatement.services.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, String> {}
