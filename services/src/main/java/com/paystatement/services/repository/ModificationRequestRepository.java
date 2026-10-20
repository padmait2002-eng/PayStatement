package com.paystatement.services.repository;

import com.paystatement.services.model.ModificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ModificationRequestRepository extends JpaRepository<ModificationRequest, Long> {
    List<ModificationRequest> findByEmployeeIdOrderBySubmittedAtDesc(String employeeId);
    List<ModificationRequest> findAllByOrderBySubmittedAtDesc();
    List<ModificationRequest> findByEmployeeIdAndWeekEndingAndStatus(String employeeId, LocalDate weekEnding, String status);
}
