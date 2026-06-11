package com.tecngo.reports.repository;
import com.tecngo.reports.entity.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserReportRepository extends JpaRepository<UserReport, UUID> {
    List<UserReport> findAllByOrderByCreatedAtDesc();
}
