package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkImportResult {
    private boolean success;
    private String errorCsvContent; // populated if success is false

    public static BulkImportResult success() {
        return new BulkImportResult(true, null);
    }

    public static BulkImportResult failure(String errorCsvContent) {
        return new BulkImportResult(false, errorCsvContent);
    }
}
