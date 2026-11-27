package com.smartstock.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.smartstock.common.ApiException;

@Entity
@Table(name = "products")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Category category;

	private String name;

	private String description;

	private String barcode;

	private BigDecimal price;

	private int stock;

	private int reorderLevel;

	private LocalDate expiryDate;

	private String warningLabel;

	private String imageName;

	private boolean active = true;

	/** Detects edits that race with another edit; stock changes always run under a row lock. */
	@Version
	private long version;

	@Column(updatable = false)
	private Instant createdAt = Instant.now();

	private Instant updatedAt = createdAt;

	protected Product() {
	}

	public Product(Category category, String name, int stock) {
		this.category = category;
		this.name = name;
		this.stock = stock;
	}

	@PreUpdate
	void touch() {
		updatedAt = Instant.now();
	}

	public boolean isExpiredOn(LocalDate today) {
		return expiryDate != null && expiryDate.isBefore(today);
	}

	public boolean isLowStock() {
		return stock <= reorderLevel;
	}

	public void addStock(int quantity) {
		stock += quantity;
	}

	public void removeStock(int quantity) {
		if (quantity > stock) {
			throw ApiException.rejected("Insufficient stock for \"%s\": %d available".formatted(name, stock));
		}
		stock -= quantity;
	}

	public void update(Category category, String name, String description, String barcode, BigDecimal price,
			int reorderLevel, LocalDate expiryDate, String warningLabel, boolean active) {
		this.category = category;
		this.name = name;
		this.description = description;
		this.barcode = barcode;
		this.price = price;
		this.reorderLevel = reorderLevel;
		this.expiryDate = expiryDate;
		this.warningLabel = warningLabel;
		this.active = active;
	}

	public Long getId() {
		return id;
	}

	public Category getCategory() {
		return category;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getBarcode() {
		return barcode;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public int getStock() {
		return stock;
	}

	public int getReorderLevel() {
		return reorderLevel;
	}

	public LocalDate getExpiryDate() {
		return expiryDate;
	}

	public String getWarningLabel() {
		return warningLabel;
	}

	public String getImageName() {
		return imageName;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
