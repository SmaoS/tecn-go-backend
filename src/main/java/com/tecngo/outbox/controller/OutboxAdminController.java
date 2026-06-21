package com.tecngo.outbox.controller;

import com.tecngo.outbox.dto.OutboxEventResponse;
import com.tecngo.outbox.dto.OutboxSummaryResponse;
import com.tecngo.outbox.service.OutboxAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/reliability/outbox")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class OutboxAdminController {
    private final OutboxAdminService service;

    @GetMapping("/summary")
    public OutboxSummaryResponse summary() {
        return service.summary();
    }

    @GetMapping("/dead")
    public List<OutboxEventResponse> dead(@RequestParam(defaultValue = "50") int limit) {
        return service.dead(limit);
    }

    @PutMapping("/{id}/retry")
    public OutboxEventResponse retry(@PathVariable UUID id) {
        return service.retry(id);
    }
}
