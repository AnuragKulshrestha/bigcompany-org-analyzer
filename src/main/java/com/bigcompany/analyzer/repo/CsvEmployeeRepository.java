package com.bigcompany.analyzer.repo;

import com.bigcompany.analyzer.model.Employee;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CsvEmployeeRepository {
    private static final Logger LOG = Logger.getLogger(CsvEmployeeRepository.class.getName());
    private final long maxFileSizeBytes;

    public CsvEmployeeRepository(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public List<Employee> load(Path csvPath) throws IOException {
        if (!Files.exists(csvPath) || !Files.isRegularFile(csvPath)) {
            throw new IOException("CSV file does not exist or is not a regular file: " + csvPath);
        }
        long size = Files.size(csvPath);
        if (size > maxFileSizeBytes) {
            throw new IOException("CSV file too large: " + size + " bytes (limit " + maxFileSizeBytes + ")");
        }

        List<Employee> result = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; } // skip header
                if (line.isBlank()) continue;
                String[] cols = line.split(",", -1);
                if (cols.length < 4) {
                    LOG.log(Level.WARNING, "Skipping malformed line (too few columns): {0}", line);
                    continue;
                }
                try {
                    String id = cols[0].trim();
                    String firstName = cols[1].trim();
                    String lastName = cols[2].trim();
                    BigDecimal salary = new BigDecimal(cols[3].trim());
                    Optional<String> managerId = Optional.empty();
                    if (cols.length >=5 && !cols[4].isBlank()) managerId = Optional.of(cols[4].trim());
                    Employee e = new Employee(id, firstName, lastName, salary, managerId);
                    result.add(e);
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Skipping malformed line (parse error): {0} -> {1}", new Object[]{line, ex.getMessage()});
                }
            }
        }
        return result;
    }
}
