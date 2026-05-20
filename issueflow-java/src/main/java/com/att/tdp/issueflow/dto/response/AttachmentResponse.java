package com.att.tdp.issueflow.dto.response;

import com.att.tdp.issueflow.entity.Attachment;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Public view of an {@link Attachment}'s metadata.
 * Does NOT contain the binary file data.
 */
@Getter
@Builder
public class AttachmentResponse {

    private final Long id;
    private final Long ticketId;
    private final String filename;
    private final String contentType;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private final Instant uploadedAt;

    public static AttachmentResponse fromEntity(Attachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .ticketId(attachment.getTicket().getId())
                .filename(attachment.getFilename())
                .contentType(attachment.getContentType())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }
}
