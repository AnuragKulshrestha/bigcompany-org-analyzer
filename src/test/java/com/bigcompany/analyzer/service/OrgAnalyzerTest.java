package com.bigcompany.analyzer.service;

import com.bigcompany.analyzer.model.Employee;
import com.bigcompany.analyzer.policy.PolicyConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class OrgAnalyzerTest {
    @Test
    void testSalaryViolations() {
        Employee e1 = new Employee("1", "CEO", "One", new BigDecimal("200000"), Optional.empty());
        Employee m1 = new Employee("2", "Manager", "A", new BigDecimal("90000"), Optional.of("1"));
        Employee s1 = new Employee("3", "Sub", "X", new BigDecimal("40000"), Optional.of("2"));
        Employee s2 = new Employee("4", "Sub", "Y", new BigDecimal("40000"), Optional.of("2"));
        PolicyConfig policy = new PolicyConfig(new BigDecimal("1.2"), new BigDecimal("1.5"), 4);
        OrgAnalyzer analyzer = new OrgAnalyzer(policy);
        List<OrgAnalyzer.SalaryViolation> vs = analyzer.analyzeSalaryCompliance(List.of(e1, m1, s1, s2));
        assertEquals(1, vs.size());
        OrgAnalyzer.SalaryViolation v = vs.get(0);
        assertFalse(v.underpaid);
        assertEquals(new BigDecimal("30000.00"), v.diff);
    }

    @Test
    void testReportingDepth() {
        Employee e1 = new Employee("1", "CEO", "One", new BigDecimal("200000"), Optional.empty());
        Employee a = new Employee("2", "A", "", new BigDecimal("50000"), Optional.of("1"));
        Employee b = new Employee("3", "B", "", new BigDecimal("50000"), Optional.of("2"));
        Employee c = new Employee("4", "C", "", new BigDecimal("50000"), Optional.of("3"));
        Employee d = new Employee("5", "D", "", new BigDecimal("50000"), Optional.of("4"));
        Employee e = new Employee("6", "E", "", new BigDecimal("50000"), Optional.of("5"));
        PolicyConfig policy = new PolicyConfig(new BigDecimal("1.2"), new BigDecimal("1.5"), 4);
        OrgAnalyzer analyzer = new OrgAnalyzer(policy);
        List<OrgAnalyzer.ReportingViolation> vs = analyzer.analyzeReportingDepth(List.of(e1,a,b,c,d,e));
        assertTrue(vs.stream().anyMatch(r -> r.employee.id().equals("6") && r.exceedBy==1));
    }
}
