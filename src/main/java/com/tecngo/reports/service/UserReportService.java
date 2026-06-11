package com.tecngo.reports.service;
import com.tecngo.reports.dto.*;
import com.tecngo.reports.entity.*;
import com.tecngo.reports.repository.UserReportRepository;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.shared.exception.*;
import com.tecngo.users.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class UserReportService {
    private final UserReportRepository reports;
    private final ServiceRequestRepository requests;
    @Transactional
    public UserReportResponse create(UUID requestId, CreateReportRequest input, User reporter) {
        ServiceRequest request = requests.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        User reported;
        if (request.getClient().getId().equals(reporter.getId()) && request.getTechnician() != null) {
            reported = request.getTechnician();
        } else if (request.getTechnician() != null && request.getTechnician().getId().equals(reporter.getId())) {
            reported = request.getClient();
        } else throw new ForbiddenException("Only service participants can report each other");
        return map(reports.save(UserReport.builder().serviceRequest(request).reporter(reporter).reported(reported)
                .reporterRole(reporter.getRole()).reportedRole(reported.getRole()).reason(input.reason())
                .description(input.description().trim()).severity(
                        input.severity() == null ? ReportSeverity.MEDIUM : input.severity()).build()));
    }
    @Transactional(readOnly = true)
    public List<UserReportResponse> list(User user) {
        requireStaff(user);
        return reports.findAllByOrderByCreatedAtDesc().stream().map(this::map).toList();
    }
    @Transactional(readOnly = true)
    public UserReportResponse find(UUID id, User user) {
        requireStaff(user);
        return map(require(id));
    }
    @Transactional
    public UserReportResponse update(UUID id, UpdateReportRequest input, User user) {
        requireStaff(user);
        if (user.getRole() == Role.VERIFIER && input.status() != ReportStatus.UNDER_REVIEW)
            throw new ForbiddenException("Verifier can only mark reports under review");
        UserReport report = require(id);
        report.setStatus(input.status());
        report.setReviewedBy(user);
        report.setReviewedAt(Instant.now());
        report.setResolutionComment(clean(input.comment()));
        return map(report);
    }
    @Transactional
    public UserReportResponse resolve(UUID id, UpdateReportRequest input, User admin) {
        if (admin.getRole() != Role.ADMIN) throw new ForbiddenException("Admin role is required");
        UserReport report = require(id);
        report.setStatus(ReportStatus.RESOLVED);
        report.setReviewedBy(admin);
        report.setReviewedAt(Instant.now());
        report.setResolutionComment(clean(input.comment()));
        return map(report);
    }
    private UserReport require(UUID id) {
        return reports.findById(id).orElseThrow(() -> new NotFoundException("Report not found"));
    }
    private void requireStaff(User user) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.VERIFIER)
            throw new ForbiddenException("Admin or verifier role is required");
    }
    private UserReportResponse map(UserReport item) {
        return new UserReportResponse(item.getId(), item.getServiceRequest().getId(),
                item.getReporter().getId(), item.getReporter().getFullName(),
                item.getReported().getId(), item.getReported().getFullName(),
                item.getReporterRole(), item.getReportedRole(), item.getReason(), item.getDescription(),
                item.getStatus(), item.getSeverity(), item.getCreatedAt(),
                item.getReviewedBy() == null ? null : item.getReviewedBy().getId(),
                item.getReviewedAt(), item.getResolutionComment());
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
