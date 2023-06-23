package com.fedex.assessment.dsabia.apiaggregation.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

@Data
@Builder
public class Aggregation {
    final Map<String, Collection<String>> shipments;
    final Map<String, String> track;
    final Map<String, BigDecimal> pricing;
}
