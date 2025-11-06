package com.bigcompany.analyzer.model;

import java.math.BigDecimal;
import java.util.Optional;

public record Employee(String id, String firstName, String lastName, BigDecimal salary, Optional<String> managerId) {
}
