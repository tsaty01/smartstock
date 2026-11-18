package com.smartstock.billing;

import java.math.BigDecimal;
import java.util.List;

import com.smartstock.inventory.Product;

/** Priced basket: amounts are rounded to paise and satisfy total = subtotal - discount + tax. */
public record Bill(List<Line> lines, BigDecimal subtotal, BigDecimal discount, DiscountSource discountSource,
		BigDecimal taxRate, BigDecimal tax, BigDecimal total) {

	public enum DiscountSource {

		NONE, RULE, MANUAL

	}

	public record Line(Product product, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
	}

}
