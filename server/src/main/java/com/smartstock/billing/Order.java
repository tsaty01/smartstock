package com.smartstock.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.smartstock.auth.User;

@Entity
@Table(name = "orders")
public class Order {

	public enum PaymentMode {

		CASH, CARD, UPI, OTHER

	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private User customer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private User cashier;

	@Enumerated(EnumType.STRING)
	private PaymentMode paymentMode;

	private BigDecimal subtotal;

	private BigDecimal discount;

	private BigDecimal taxRate;

	private BigDecimal tax;

	private BigDecimal total;

	@Column(updatable = false)
	private Instant createdAt = Instant.now();

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	protected Order() {
	}

	Order(User customer, User cashier, PaymentMode paymentMode, Bill bill) {
		this.customer = customer;
		this.cashier = cashier;
		this.paymentMode = paymentMode;
		this.subtotal = bill.subtotal();
		this.discount = bill.discount();
		this.taxRate = bill.taxRate();
		this.tax = bill.tax();
		this.total = bill.total();
		bill.lines().forEach(line -> items.add(new OrderItem(this, line)));
	}

	public Long getId() {
		return id;
	}

	public User getCustomer() {
		return customer;
	}

	public User getCashier() {
		return cashier;
	}

	public PaymentMode getPaymentMode() {
		return paymentMode;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}

	public BigDecimal getDiscount() {
		return discount;
	}

	public BigDecimal getTaxRate() {
		return taxRate;
	}

	public BigDecimal getTax() {
		return tax;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public List<OrderItem> getItems() {
		return items;
	}

}
