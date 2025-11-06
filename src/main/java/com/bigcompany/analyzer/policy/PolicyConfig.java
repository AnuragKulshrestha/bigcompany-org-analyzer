package com.bigcompany.analyzer.policy;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PolicyConfig {
    private final BigDecimal minMultiplier;
    private final BigDecimal maxMultiplier;
    private final int maxReportingDepth;

    public PolicyConfig(BigDecimal minMultiplier, BigDecimal maxMultiplier, int maxReportingDepth) {
        this.minMultiplier = minMultiplier;
        this.maxMultiplier = maxMultiplier;
        this.maxReportingDepth = maxReportingDepth;
    }

    public static PolicyConfig loadFrom(Path propertiesPath) throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(propertiesPath)) {
            p.load(in);
        }
        BigDecimal min = new BigDecimal(p.getProperty("policy.minMultiplier", "1.2"));
        BigDecimal max = new BigDecimal(p.getProperty("policy.maxMultiplier", "1.5"));
        int depth = Integer.parseInt(p.getProperty("policy.maxReportingDepth", "4"));
        return new PolicyConfig(min, max, depth);
    }

    public BigDecimal minMultiplier() { return minMultiplier; }
    public BigDecimal maxMultiplier() { return maxMultiplier; }
    public int maxReportingDepth() { return maxReportingDepth; }
}
