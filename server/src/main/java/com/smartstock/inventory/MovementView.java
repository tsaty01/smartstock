package com.smartstock.inventory;

import java.time.Instant;

import com.smartstock.inventory.StockMovement.Reason;

public record MovementView(Long id, Long productId, String productName, int change, Reason reason, Long orderId,
		String performedBy, String notes, Instant createdAt) {
}
