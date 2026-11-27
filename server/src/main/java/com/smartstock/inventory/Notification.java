package com.smartstock.inventory;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
public class Notification {

	public enum Type {

		LOW_STOCK, EXPIRY

	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private Type type;

	private String message;

	private Long productId;

	private boolean read;

	@Column(updatable = false)
	private Instant createdAt = Instant.now();

	protected Notification() {
	}

	Notification(Type type, String message, Long productId) {
		this.type = type;
		this.message = message;
		this.productId = productId;
	}

	public void markRead() {
		read = true;
	}

	public Long getId() {
		return id;
	}

	public Type getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}

	public Long getProductId() {
		return productId;
	}

	public boolean isRead() {
		return read;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
