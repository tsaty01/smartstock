package com.smartstock.billing;

import java.math.BigDecimal;
import java.time.Instant;

import com.smartstock.billing.Order.PaymentMode;

public record OrderSummary(Long id, Instant createdAt, Long customerId, String customerName, String cashierName,
		PaymentMode paymentMode, int itemCount, BigDecimal total) {
}
