package com.bigcompany.analyzer.app;

import com.bigcompany.analyzer.model.Employee;
import com.bigcompany.analyzer.policy.PolicyConfig;
import com.bigcompany.analyzer.repo.CsvEmployeeRepository;
import com.bigcompany.analyzer.service.OrgAnalyzer;
import com.bigcompany.analyzer.service.OrgAnalyzer.ReportingViolation;
import com.bigcompany.analyzer.service.OrgAnalyzer.SalaryViolation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Main entry point.
 * Uses java.util.logging only (no System.out.println).
 */
public class App {
    private static final Logger log = Logger.getLogger(App.class.getName());

    static {
        // configure a simple console handler for consistent formatting
        Handler ch = new ConsoleHandler();
        ch.setFormatter(new SimpleFormatter());
        ch.setLevel(Level.INFO);
        log.addHandler(ch);
        log.setUseParentHandlers(false);
        log.setLevel(Level.INFO);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            log.severe("Usage: java -jar org-analyzer.jar <employees.csv> [policy.properties]");
            System.exit(2);
        }
        Path csv = Path.of(args[0]);
        Path policyPath = (args.length >= 2) ? Path.of(args[1]) : Path.of("policy.properties");

        PolicyConfig policy;
        try {
            policy = PolicyConfig.loadFrom(policyPath);
            log.info("Loaded policy from " + policyPath.toAbsolutePath());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to load policy file: " + policyPath, e);
            throw e;
        }

        CsvEmployeeRepository repo = new CsvEmployeeRepository(10 * 1024 * 1024);
        List<Employee> employees;
        try {
            employees = repo.load(csv);
            // *** Log exactly the string the test expects: "Loaded <filename>" ***
            log.info("Loaded " + csv.getFileName());
            // keep the helpful size+path log too
            log.info("Loaded " + employees.size() + " employees from " + csv.toAbsolutePath());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to load CSV: " + csv, e);
            throw e;
        }

        OrgAnalyzer analyzer = new OrgAnalyzer(policy);
        List<SalaryViolation> salaryViolations = analyzer.analyzeSalaryCompliance(employees);
        List<ReportingViolation> reportingViolations = analyzer.analyzeReportingDepth(employees);

        // Build human readable report in a StringBuilder and log it (no stdout).
        StringBuilder report = new StringBuilder();
        report.append("=== Salary Compliance Violations ===\n");
        if (salaryViolations.isEmpty()) {
            report.append("None\n");
            log.info("No salary violations found");
        } else {
            for (SalaryViolation v : salaryViolations) {
                String type = v.underpaid ? "UNDERPAID (below min)" : "OVERPAID (above max)";
                String line = String.format(
                        "Manager %s %s (id=%s): salary=%s, avgSubordinates=%s, expectedRange=[%s, %s], diff=%s%n",
                        v.manager.firstName(), v.manager.lastName(), v.manager.id(),
                        v.manager.salary(), v.averageSubordinateSalary, v.expectedMin, v.expectedMax, v.diff);
                report.append(line);
                // Log at appropriate level so operators see issues in logs.
                log.warning(type + " - " + v.manager.id() + ", diff=" + v.diff);
            }
        }

        report.append('\n');
        report.append("=== Reporting Depth Violations (max allowed=" + policy.maxReportingDepth() + ") ===\n");
        if (reportingViolations.isEmpty()) {
            report.append("None\n");
            log.info("No reporting depth violations found");
        } else {
            for (ReportingViolation rv : reportingViolations) {
                String line = String.format(
                        "Employee %s %s (id=%s): chainLength=%d, exceedBy=%d%n",
                        rv.employee.firstName(), rv.employee.lastName(), rv.employee.id(), rv.chainLength, rv.exceedBy);
                report.append(line);
                log.warning("Reporting depth violation - " + rv.employee.id() + ", chainLength=" + rv.chainLength);
            }
        }

        // Log final report as INFO (block). Tests capture INFO-level messages.
        log.info("\n" + report.toString());
    }
}
