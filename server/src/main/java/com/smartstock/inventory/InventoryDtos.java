package com.smartstock.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.smartstock.inventory.StockMovement.Reason;

public final class InventoryDtos {

	private InventoryDtos() {
	}

	public record CategoryRequest(@NotBlank @Size(max = 100) String name, @Size(max = 255) String description) {
	}

	public record CategoryView(Long id, String name, String description) {

		public static CategoryView of(Category category) {
			return new CategoryView(category.getId(), category.getName(), category.getDescription());
		}

	}

	/**
	 * Full product representation for create and replace. Stock is changed only through restock and
	 * adjustments; {@code initialStock} applies on create only and defaults to zero.
	 */
	public record ProductRequest(
			@NotNull Long categoryId,
			@NotBlank @Size(max = 150) String name,
			String description,
			@Size(max = 50) String barcode,
			@NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal price,
			@PositiveOrZero int reorderLevel,
			LocalDate expiryDate,
			@Size(max = 100) String warningLabel,
			boolean active,
			@PositiveOrZero Integer initialStock) {

		int initialStockOrZero() {
			return initialStock == null ? 0 : initialStock;
		}

	}

	public record ProductView(Long id, Long categoryId, String categoryName, String name, String description,
			String barcode, BigDecimal price, int stock, int reorderLevel, LocalDate expiryDate, Long daysToExpiry,
			String warningLabel, String imageUrl, boolean active, boolean lowStock, boolean expired,
			Instant updatedAt) {

		public static ProductView of(Product p, LocalDate today) {
			Long daysToExpiry = p.getExpiryDate() == null ? null : ChronoUnit.DAYS.between(today, p.getExpiryDate());
			String imageUrl = p.getImageName() == null ? null : "/api/products/%d/image?v=%s".formatted(p.getId(), p.getImageName());
			return new ProductView(p.getId(), p.getCategory().getId(), p.getCategory().getName(), p.getName(),
					p.getDescription(), p.getBarcode(), p.getPrice(), p.getStock(), p.getReorderLevel(),
					p.getExpiryDate(), daysToExpiry, p.getWarningLabel(), imageUrl, p.isActive(), p.isLowStock(),
					p.isExpiredOn(today), p.getUpdatedAt());
		}

	}

	public record RestockRequest(@Positive int quantity, @Size(max = 255) String notes) {
	}

	/** @param change signed stock change; only ADJUSTMENT and EXPIRED_REMOVAL are accepted */
	public record AdjustmentRequest(int change, @NotNull Reason reason, @NotBlank @Size(max = 255) String notes) {
	}

	public record NotificationView(Long id, Notification.Type type, String message, Long productId, boolean read,
			Instant createdAt) {

		public static NotificationView of(Notification n) {
			return new NotificationView(n.getId(), n.getType(), n.getMessage(), n.getProductId(), n.isRead(),
					n.getCreatedAt());
		}

	}

}
