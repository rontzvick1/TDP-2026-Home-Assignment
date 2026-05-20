package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.dto.response.CsvImportResponse;
import com.att.tdp.issueflow.dto.response.TicketResponse;
import com.att.tdp.issueflow.entity.TicketPriority;
import com.att.tdp.issueflow.entity.TicketStatus;
import com.att.tdp.issueflow.entity.TicketType;
import com.att.tdp.issueflow.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service handling CSV Export and Import for tickets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvService {

    private final TicketService ticketService;

    // We only export standard fields (ignoring dueDate for simplicity as per plan)
    private static final String[] HEADERS = {"id", "title", "description", "status", "priority", "type", "assigneeId"};

    /**
     * Exports active tickets for a project as a CSV string.
     */
    public String exportTicketsToCsv(Long projectId) {
        List<TicketResponse> tickets = ticketService.getTicketsByProject(projectId);

        try (StringWriter sw = new StringWriter();
             CSVPrinter csvPrinter = new CSVPrinter(sw, CSVFormat.DEFAULT.builder()
                     .setHeader(HEADERS)
                     .build())) {

            for (TicketResponse ticket : tickets) {
                csvPrinter.printRecord(
                        ticket.getId(),
                        ticket.getTitle(),
                        ticket.getDescription() != null ? ticket.getDescription() : "",
                        ticket.getStatus(),
                        ticket.getPriority(),
                        ticket.getType(),
                        ticket.getAssigneeId() != null ? ticket.getAssigneeId() : ""
                );
            }
            csvPrinter.flush();
            return sw.toString();

        } catch (IOException e) {
            log.error("Error generating CSV", e);
            throw new RuntimeException("Failed to generate CSV data");
        }
    }

    /**
     * Imports tickets from a CSV file.
     * Each valid row creates a new ticket. Fails gracefully row-by-row.
     */
    public CsvImportResponse importTicketsFromCsv(Long projectId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("CSV file is empty");
        }

        int createdCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            for (CSVRecord csvRecord : csvParser) {
                long rowNum = csvRecord.getRecordNumber();
                try {
                    // Extract fields mapping to CreateTicketRequest
                    String title = csvRecord.get("title");
                    String description = csvRecord.isSet("description") ? csvRecord.get("description") : null;
                    String statusStr = csvRecord.get("status");
                    String priorityStr = csvRecord.get("priority");
                    String typeStr = csvRecord.get("type");
                    
                    String assigneeStr = csvRecord.isSet("assigneeId") ? csvRecord.get("assigneeId") : null;
                    Long assigneeId = (assigneeStr != null && !assigneeStr.isBlank()) ? Long.parseLong(assigneeStr) : null;

                    if (title == null || title.isBlank()) {
                        throw new IllegalArgumentException("Title is required");
                    }

                    // Create the request DTO natively (this bypasses standard @Valid annotations,
                    // but TicketService + entities validate non-null DB constraints anyway,
                    // and we map enums manually here).
                    // Since CreateTicketRequest doesn't have a builder (it's built for JSON mapping),
                    // we instantiate it using reflection or we just construct a custom flow.
                    // To keep it simple and reuse standard @Valid flow, we'll manually instantiate the DTO.

                    CreateTicketRequest request = new CreateTicketRequest();
                    request.setTitle(title);
                    request.setDescription(description);
                    request.setStatus(TicketStatus.valueOf(statusStr.toUpperCase()));
                    request.setPriority(TicketPriority.valueOf(priorityStr.toUpperCase()));
                    request.setType(TicketType.valueOf(typeStr.toUpperCase()));
                    request.setProjectId(projectId);
                    request.setAssigneeId(assigneeId);
                    request.setDueDate(null);

                    ticketService.createTicket(request);
                    createdCount++;

                } catch (IllegalArgumentException e) {
                    failedCount++;
                    errors.add("Row " + rowNum + " Enum/Format error: " + e.getMessage());
                } catch (Exception e) {
                    failedCount++;
                    errors.add("Row " + rowNum + " error: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            log.error("Failed to parse CSV file", e);
            throw new ValidationException("Failed to parse CSV file: " + e.getMessage());
        }

        return CsvImportResponse.builder()
                .created(createdCount)
                .failed(failedCount)
                .errors(errors)
                .build();
    }
}
