package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.dto.request.UpdateTicketRequest;
import com.att.tdp.issueflow.dto.response.CsvImportResponse;
import com.att.tdp.issueflow.dto.response.TicketResponse;
import com.att.tdp.issueflow.service.CsvService;
import com.att.tdp.issueflow.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for the Tickets API.
 *
 * <h3>⚠️ Route ordering is critical</h3>
 * Spring MVC resolves literal path segments before path variables, but to be explicit
 * and avoid any ambiguity, specific routes ({@code /deleted}, {@code /export},
 * {@code /import}) are declared <em>above</em> the parameterised {@code /{ticketId}}
 * handler. This prevents "deleted" or "export" from ever being parsed as a {@code Long}.
 *
 * <p>CSV export/import routes ({@code GET /tickets/export}, {@code POST /tickets/import})
 * are implemented in Phase 11 (CsvService). The stubs here maintain correct ordering.</p>
 *
 * <table>
 *   <tr><th>Method</th><th>Path</th><th>Auth</th><th>Description</th></tr>
 *   <tr><td>GET</td><td>/tickets/deleted</td><td>ADMIN</td><td>List soft-deleted tickets for a project</td></tr>
 *   <tr><td>GET</td><td>/tickets/export</td><td>Any</td><td>Export tickets to CSV (Phase 11)</td></tr>
 *   <tr><td>POST</td><td>/tickets/import</td><td>Any</td><td>Import tickets from CSV (Phase 11)</td></tr>
 *   <tr><td>GET</td><td>/tickets</td><td>Any</td><td>List active tickets for a project</td></tr>
 *   <tr><td>GET</td><td>/tickets/{ticketId}</td><td>Any</td><td>Get ticket by ID</td></tr>
 *   <tr><td>POST</td><td>/tickets</td><td>Any</td><td>Create ticket</td></tr>
 *   <tr><td>PATCH</td><td>/tickets/{ticketId}</td><td>Any</td><td>Partially update ticket</td></tr>
 *   <tr><td>DELETE</td><td>/tickets/{ticketId}</td><td>Any</td><td>Soft-delete ticket</td></tr>
 *   <tr><td>POST</td><td>/tickets/{ticketId}/restore</td><td>ADMIN</td><td>Restore soft-deleted ticket</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final CsvService csvService;

    // ═══════════════════════════════════════════════════════════════════════
    // ── IMPORTANT: literal routes declared FIRST to avoid path-var collision
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ADMIN: lists all soft-deleted tickets for a project.
     * Route "/deleted" must be declared before "/{ticketId}".
     */
    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TicketResponse>> getDeletedTickets(
            @RequestParam Long projectId) {
        return ResponseEntity.ok(ticketService.getDeletedTickets(projectId));
    }

    // ── CSV Endpoints (Phase 11) — declared here to maintain correct ordering ──
    
    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> exportTickets(@RequestParam Long projectId) {
        String csvContent = csvService.exportTicketsToCsv(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tickets_project_" + projectId + ".csv\"")
                .body(csvContent);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CsvImportResponse> importTickets(
            @RequestParam Long projectId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(csvService.importTicketsFromCsv(projectId, file));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ── Standard CRUD
    // ═══════════════════════════════════════════════════════════════════════

    /** Lists all non-deleted tickets for a given project, each with {@code isOverdue}. */
    @GetMapping
    public ResponseEntity<List<TicketResponse>> getTicketsByProject(
            @RequestParam Long projectId) {
        return ResponseEntity.ok(ticketService.getTicketsByProject(projectId));
    }

    /** Returns a single non-deleted ticket by ID, including {@code isOverdue}. */
    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.getTicketById(ticketId));
    }

    /**
     * Creates a new ticket.
     *
     * <p>If {@code assigneeId} is absent or {@code null}, auto-assignment kicks in:
     * the DEVELOPER with the fewest open tickets is assigned automatically.</p>
     */
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.ok(ticketService.createTicket(request));
    }

    /** Partially updates a ticket. Only non-null fields in the body are applied. */
    @PatchMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketRequest request) {
        return ResponseEntity.ok(ticketService.updateTicket(ticketId, request));
    }

    /** Soft-deletes a ticket by setting {@code deletedAt}. */
    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> softDeleteTicket(@PathVariable Long ticketId) {
        ticketService.softDeleteTicket(ticketId);
        return ResponseEntity.ok().build();
    }

    /**
     * ADMIN: restores a soft-deleted ticket by clearing {@code deletedAt}.
     * Idempotent — restoring an already-active ticket is a no-op.
     */
    @PostMapping("/{ticketId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> restoreTicket(@PathVariable Long ticketId) {
        ticketService.restoreTicket(ticketId);
        return ResponseEntity.ok().build();
    }
}
