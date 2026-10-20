package com.paystatement.services.repository;

import com.paystatement.services.model.WeeklyHours;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyHoursRepository extends JpaRepository<WeeklyHours, Long> {
    Optional<WeeklyHours> findByEmployeeIdAndWeekEnding(String employeeId, LocalDate weekEnding);
}
