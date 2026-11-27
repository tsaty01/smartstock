package com.smartstock.billing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.smartstock.billing.Bill.DiscountSource;
import com.smartstock.billing.Bill.Line;
import com.smartstock.common.ApiException;
import com.smartstock.common.Money;
import com.smartstock.common.SmartstockProperties;
import com.smartstock.inventory.Product;

/** The only place sale amounts are computed; quotes and checkouts both use it. */
@Component
class BillCalculator {

	private final DiscountRuleRepository discountRules;

	private final BigDecimal taxRate;

	private final Clock clock;

	BillCalculator(DiscountRuleRepository discountRules, SmartstockProperties properties, Clock clock) {
		this.discountRules = discountRules;
		this.taxRate = properties.billing().taxRate();
		this.clock = clock;
	}

	/**
	 * Prices sellable products. A manual discount replaces the automatic rule discount; tax applies to the
	 * discounted subtotal.
	 */
	Bill price(List<Product> products, Map<Long, Integer> quantities, BigDecimal manualDiscount) {
		LocalDate today = LocalDate.now(clock);
		List<Line> lines = products.stream().map(p -> {
			requireSellable(p, quantities.get(p.getId()), today);
			int quantity = quantities.get(p.getId());
			return new Line(p, quantity, p.getPrice(), Money.round(p.getPrice().multiply(BigDecimal.valueOf(quantity))));
		}).toList();
		BigDecimal subtotal = lines.stream().map(Line::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		int totalQuantity = lines.stream().mapToInt(Line::quantity).sum();

		BigDecimal discount;
		DiscountSource source;
		if (manualDiscount != null) {
			discount = Money.round(manualDiscount);
			if (discount.compareTo(subtotal) > 0) {
				throw ApiException.rejected("Discount cannot exceed the subtotal");
			}
			source = DiscountSource.MANUAL;
		}
		else {
			BigDecimal percent = discountRules.findBestPercent(totalQuantity, subtotal, today).orElse(BigDecimal.ZERO);
			discount = Money.percentOf(subtotal, percent);
			source = discount.signum() > 0 ? DiscountSource.RULE : DiscountSource.NONE;
		}
		BigDecimal tax = Money.percentOf(subtotal.subtract(discount), taxRate);
		BigDecimal total = subtotal.subtract(discount).add(tax);
		return new Bill(lines, Money.round(subtotal), discount, source, taxRate, tax, Money.round(total));
	}

	private static void requireSellable(Product product, int quantity, LocalDate today) {
		if (!product.isActive()) {
			throw ApiException.rejected("\"%s\" is not available for sale".formatted(product.getName()));
		}
		if (product.isExpiredOn(today)) {
			throw ApiException.rejected("\"%s\" is expired and cannot be sold".formatted(product.getName()));
		}
		if (product.getStock() < quantity) {
			throw ApiException.rejected("Insufficient stock for \"%s\": %d available".formatted(product.getName(),
					product.getStock()));
		}
	}

}
