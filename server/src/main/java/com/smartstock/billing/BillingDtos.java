package com.smartstock.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.smartstock.billing.Bill.DiscountSource;
import com.smartstock.billing.Order.PaymentMode;

public final class BillingDtos {

	private BillingDtos() {
	}

	public record CartItem(@NotNull Long productId, @Positive @Max(10_000) int quantity) {
	}

	/**
	 * @param discount manual discount amount (managers only); null applies the best automatic rule
	 */
	public record CheckoutRequest(
			@NotNull Long customerId,
			@NotEmpty @Size(max = 200) List<@Valid @NotNull CartItem> items,
			@NotNull PaymentMode paymentMode,
			@PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal discount) {
	}

	public record QuoteRequest(
			@NotEmpty @Size(max = 200) List<@Valid @NotNull CartItem> items,
			@PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal discount) {
	}

	public record LineView(Long productId, String productName, int quantity, BigDecimal unitPrice,
			BigDecimal lineTotal) {
	}

	public record QuoteView(List<LineView> items, BigDecimal subtotal, BigDecimal discount,
			DiscountSource discountSource, BigDecimal taxRate, BigDecimal tax, BigDecimal total) {

		static QuoteView of(Bill bill) {
			List<LineView> lines = bill.lines().stream()
				.map(l -> new LineView(l.product().getId(), l.product().getName(), l.quantity(), l.unitPrice(),
						l.lineTotal()))
				.toList();
			return new QuoteView(lines, bill.subtotal(), bill.discount(), bill.discountSource(), bill.taxRate(),
					bill.tax(), bill.total());
		}

	}

	public record ReceiptView(Long id, Instant createdAt, Long customerId, String customerName, String customerPhone,
			String cashierName, PaymentMode paymentMode, List<LineView> items, BigDecimal subtotal,
			BigDecimal discount, BigDecimal taxRate, BigDecimal tax, BigDecimal total) {

		static ReceiptView of(Order o) {
			List<LineView> lines = o.getItems().stream()
				.map(i -> new LineView(i.getProductId(), i.getProductName(), i.getQuantity(), i.getUnitPrice(),
						i.getLineTotal()))
				.toList();
			return new ReceiptView(o.getId(), o.getCreatedAt(), o.getCustomer().getId(), o.getCustomer().getFullName(),
					o.getCustomer().getPhone(), o.getCashier().getFullName(), o.getPaymentMode(), lines,
					o.getSubtotal(), o.getDiscount(), o.getTaxRate(), o.getTax(), o.getTotal());
		}

	}

	public record DiscountRuleRequest(
			@NotBlank @Size(max = 100) String name,
			@Positive int minQuantity,
			@NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal minAmount,
			@NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal percent,
			boolean active,
			LocalDate validFrom,
			LocalDate validTo) {
	}

	public record DiscountRuleView(Long id, String name, int minQuantity, BigDecimal minAmount, BigDecimal percent,
			boolean active, LocalDate validFrom, LocalDate validTo) {

		static DiscountRuleView of(DiscountRule r) {
			return new DiscountRuleView(r.getId(), r.getName(), r.getMinQuantity(), r.getMinAmount(), r.getPercent(),
					r.isActive(), r.getValidFrom(), r.getValidTo());
		}

	}

}
