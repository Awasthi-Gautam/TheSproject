package com.example.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class CsvValidationService {

    private static final String[] REQUIRED_HEADERS = {
            "aadhaar", "name", "dob", "mother_name", "father_name",
            "category_code", "status_code", "class_id", "phone", "email"
    };

    public List<CSVRecord> validateAndParse(InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            try (CSVParser parser = new CSVParser(reader, format)) {
                // Validate headers
                for (String header : REQUIRED_HEADERS) {
                    if (!parser.getHeaderMap().containsKey(header)) {
                        throw new IllegalArgumentException("Missing required header: " + header);
                    }
                }
                return parser.getRecords();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }
}
