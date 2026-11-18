package com.smartstock.web;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.smartstock.billing.BillingDtos.CartItem;
import com.smartstock.billing.Order.PaymentMode;

/** The website till's open sale, kept in the staff member's session until checkout. */
@Component
@SessionScope
public class PosCart implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private final Map<Long, Integer> quantities = new LinkedHashMap<>();

	private Long customerId;

	private PaymentMode paymentMode = PaymentMode.CASH;

	private BigDecimal discount;

	public void add(long productId, int quantity) {
		set(productId, quantities.getOrDefault(productId, 0) + quantity);
	}

	public void set(long productId, int quantity) {
		if (quantity <= 0) {
			quantities.remove(productId);
		}
		else {
			quantities.put(productId, quantity);
		}
	}

	public List<CartItem> items() {
		return quantities.entrySet().stream().map(e -> new CartItem(e.getKey(), e.getValue())).toList();
	}

	public boolean isEmpty() {
		return quantities.isEmpty();
	}

	public void clear() {
		quantities.clear();
		customerId = null;
		paymentMode = PaymentMode.CASH;
		discount = null;
	}

	public Long getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}

	public PaymentMode getPaymentMode() {
		return paymentMode;
	}

	public void setPaymentMode(PaymentMode paymentMode) {
		this.paymentMode = paymentMode;
	}

	public BigDecimal getDiscount() {
		return discount;
	}

	public void setDiscount(BigDecimal discount) {
		this.discount = discount;
	}

}
