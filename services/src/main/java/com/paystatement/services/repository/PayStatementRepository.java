package com.paystatement.services.repository;

import com.paystatement.services.model.PayStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayStatementRepository extends JpaRepository<PayStatement, Long> {
    List<PayStatement> findByEmployeeIdOrderByWeekEndingDesc(String employeeId);
    Optional<PayStatement> findByEmployeeIdAndWeekEnding(String employeeId, LocalDate weekEnding);
}
