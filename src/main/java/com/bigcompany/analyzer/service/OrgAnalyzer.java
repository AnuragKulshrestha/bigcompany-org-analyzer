package com.bigcompany.analyzer.service;

import com.bigcompany.analyzer.model.Employee;
import com.bigcompany.analyzer.policy.PolicyConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class OrgAnalyzer {
    public static final class SalaryViolation {
        public final Employee manager;
        public final BigDecimal averageSubordinateSalary;
        public final BigDecimal expectedMin;
        public final BigDecimal expectedMax;
        public final BigDecimal diff;
        public final boolean underpaid;

        public SalaryViolation(Employee manager, BigDecimal averageSubordinateSalary, BigDecimal expectedMin, BigDecimal expectedMax, BigDecimal diff, boolean underpaid) {
            this.manager = manager;
            this.averageSubordinateSalary = averageSubordinateSalary;
            this.expectedMin = expectedMin;
            this.expectedMax = expectedMax;
            this.diff = diff;
            this.underpaid = underpaid;
        }
    }

    public static final class ReportingViolation {
        public final Employee employee;
        public final int chainLength;
        public final int exceedBy;
        public ReportingViolation(Employee employee, int chainLength, int exceedBy) { this.employee = employee; this.chainLength = chainLength; this.exceedBy = exceedBy; }
    }

    private final PolicyConfig policy;

    public OrgAnalyzer(PolicyConfig policy) {
        this.policy = policy;
    }

    public List<SalaryViolation> analyzeSalaryCompliance(Collection<Employee> employees) {
        Map<String, Employee> byId = employees.stream().collect(Collectors.toMap(Employee::id, e -> e));
        // Build manager -> list of direct subordinates
        Map<String, List<Employee>> directSubs = new HashMap<>();
        Set<String> managerIds = new HashSet<>(); // ids that are managers (have direct reports)
        for (Employee e : employees) {
            e.managerId().ifPresent(mgr -> {
                directSubs.computeIfAbsent(mgr, k -> new ArrayList<>()).add(e);
                managerIds.add(mgr);
            });
        }

        List<SalaryViolation> violations = new ArrayList<>();
        for (Map.Entry<String, List<Employee>> entry : directSubs.entrySet()) {
            Employee manager = byId.get(entry.getKey());
            if (manager == null) continue; // manager not present (malformed data)
            List<Employee> subs = entry.getValue();
            // Filter to only leaf direct subordinates (those who are NOT managers themselves)
            List<Employee> leafSubs = subs.stream().filter(s -> !managerIds.contains(s.id())).collect(Collectors.toList());
            if (leafSubs.isEmpty()) {
                // If there are no leaf direct reports, skip salary compliance for this manager
                continue;
            }
            BigDecimal avg = leafSubs.stream()
                    .map(Employee::salary)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(leafSubs.size()), 2, RoundingMode.HALF_UP);
            BigDecimal expectedMin = avg.multiply(policy.minMultiplier()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal expectedMax = avg.multiply(policy.maxMultiplier()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal mgrSalary = manager.salary();
            if (mgrSalary.compareTo(expectedMin) < 0) {
                BigDecimal diff = expectedMin.subtract(mgrSalary).setScale(2, RoundingMode.HALF_UP);
                violations.add(new SalaryViolation(manager, avg, expectedMin, expectedMax, diff, true));
            } else if (mgrSalary.compareTo(expectedMax) > 0) {
                BigDecimal diff = mgrSalary.subtract(expectedMax).setScale(2, RoundingMode.HALF_UP);
                violations.add(new SalaryViolation(manager, avg, expectedMin, expectedMax, diff, false));
            }
        }
        return violations;
    }

    public List<ReportingViolation> analyzeReportingDepth(Collection<Employee> employees) {
        Map<String, Employee> byId = employees.stream().collect(Collectors.toMap(Employee::id, e -> e));
        List<ReportingViolation> violations = new ArrayList<>();
        for (Employee e : employees) {
            int depth = 0;
            Set<String> seen = new HashSet<>();
            Optional<String> curr = e.managerId();
            while (curr.isPresent()) {
                String mid = curr.get();
                if (seen.contains(mid)) {
                    depth = Integer.MAX_VALUE; break;
                }
                seen.add(mid);
                Employee mgr = byId.get(mid);
                if (mgr == null) break;
                depth++;
                curr = mgr.managerId();
            }
            if (depth > policy.maxReportingDepth()) {
                violations.add(new ReportingViolation(e, depth, depth - policy.maxReportingDepth()));
            }
        }
        return violations;
    }
}
