package com.buildtrack.model;

import java.math.BigDecimal;

public record PurchaseItem(int componentId, String componentName,
                           int quantityToPurchase, BigDecimal estimatedCost) {
}
