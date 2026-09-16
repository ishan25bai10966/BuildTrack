package com.buildtrack.model;

import java.util.Map;

public record ComponentAvailability(int totalRequiredQuantity,
                                    int availableRequiredQuantity,
                                    Map<Integer, Integer> missingQuantities) {
}
