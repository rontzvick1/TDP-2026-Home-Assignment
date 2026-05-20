package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.response.AttachmentResponse;
import com.att.tdp.issueflow.entity.Attachment;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.exception.ValidationException;
import com.att.tdp.issueflow.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Business logic for ticket attachments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TicketService      ticketService;

    @Transactional
    public AttachmentResponse uploadAttachment(Long ticketId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("Cannot upload empty file");
        }

        Ticket ticket = ticketService.getActiveTicketEntity(ticketId);
        String filename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown");

        try {
            Attachment attachment = Attachment.builder()
                    .ticket(ticket)
                    .filename(filename)
                    .contentType(file.getContentType())
                    .data(file.getBytes())
                    .build();

            attachment = attachmentRepository.save(attachment);
            return AttachmentResponse.fromEntity(attachment);

        } catch (IOException e) {
            log.error("Failed to read file payload", e);
            throw new RuntimeException("Could not store file " + filename + ". Please try again!", e);
        }
    }

    @Transactional(readOnly = true)
    public Attachment getAttachmentData(Long ticketId, Long attachmentId) {
        return attachmentRepository.findByIdAndTicketId(attachmentId, ticketId)
                .orElseThrow(() -> new NotFoundException("Attachment", attachmentId));
    }

    @Transactional
    public void deleteAttachment(Long ticketId, Long attachmentId) {
        Attachment attachment = attachmentRepository.findByIdAndTicketId(attachmentId, ticketId)
                .orElseThrow(() -> new NotFoundException("Attachment", attachmentId));

        attachmentRepository.delete(attachment);
    }
}
