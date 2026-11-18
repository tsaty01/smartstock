package com.smartstock.inventory;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.smartstock.auth.User;

/** Audit entry for every change to a product's stock. */
@Entity
@Table(name = "stock_movements")
public class StockMovement {

	public enum Reason {

		INITIAL, RESTOCK, SALE, ADJUSTMENT, EXPIRED_REMOVAL

	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Product product;

	private int change;

	@Enumerated(EnumType.STRING)
	private Reason reason;

	/** Sale order id; a plain column so inventory does not depend on billing. */
	private Long orderId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "performed_by")
	private User performedBy;

	private String notes;

	@Column(updatable = false)
	private Instant createdAt = Instant.now();

	protected StockMovement() {
	}

	public StockMovement(Product product, int change, Reason reason, Long orderId, User performedBy, String notes) {
		this.product = product;
		this.change = change;
		this.reason = reason;
		this.orderId = orderId;
		this.performedBy = performedBy;
		this.notes = notes;
	}

}
