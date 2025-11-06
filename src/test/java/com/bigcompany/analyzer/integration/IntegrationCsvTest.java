package com.bigcompany.analyzer.integration;

import com.bigcompany.analyzer.app.App;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationCsvTest {

    private static class TestHandler extends Handler {
        private final List<String> msgs = new ArrayList<>();
        @Override public void publish(LogRecord record) {
            if (record.getLevel().intValue() >= Level.INFO.intValue()) {
                msgs.add(record.getMessage());
            }
        }
        @Override public void flush() {}
        @Override public void close() throws SecurityException {}
        public List<String> getMsgs() { return msgs; }
    }

    @Test
    void runMainAgainstSampleCsv() throws Exception {
        Path tmpDir = Files.createTempDirectory("org-analyzer-test");
        Path csv = tmpDir.resolve("employees.csv");
        String csvContent = """
                Id,firstName,lastName,salary,managerId
                1,CEO,One,200000,
                2,Manager,A,90000,1
                3,Sub,X,40000,2
                4,Sub,Y,40000,2
                """;
        Files.writeString(csv, csvContent);

        Path policy = tmpDir.resolve("policy.properties");
        String policyContent = """
                policy.minMultiplier=1.2
                policy.maxMultiplier=1.5
                policy.maxReportingDepth=4
                """;
        Files.writeString(policy, policyContent);

        Logger logger = Logger.getLogger("com.bigcompany.analyzer.app.App");
        TestHandler handler = new TestHandler();
        handler.setLevel(Level.INFO);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);

        try {
            App.main(new String[]{ csv.toString(), policy.toString() });
        } finally {
            // remove handler to avoid leaking between tests
            logger.removeHandler(handler);
        }

        List<String> msgs = handler.getMsgs();
        // join messages and assert expected content present
        String joined = String.join("\n", msgs);
        assertTrue(joined.contains("Loaded " + csv.getFileName()), "Should log that CSV was loaded");
        assertTrue(joined.contains("Salary Compliance Violations") || joined.contains("Manager A"), "Should mention salary section or manager");
    }
}
