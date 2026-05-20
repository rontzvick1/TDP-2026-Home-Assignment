package com.att.tdp.issueflow.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Response body for the CSV import API.
 */
@Getter
@Builder
public class CsvImportResponse {
    private final int created;
    private final int failed;
    private final List<String> errors;
}
