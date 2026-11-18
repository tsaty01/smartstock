package com.smartstock.inventory;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

/**
 * Catalog query. {@code sellable} keeps only products that can be sold today: active, in stock and not
 * expired.
 */
public record ProductFilter(Long categoryId, String query, boolean includeInactive, boolean sellable) {

	Specification<Product> toSpecification(LocalDate today) {
		Specification<Product> spec = Specification.unrestricted();
		if (categoryId != null) {
			spec = spec.and((p, q, cb) -> cb.equal(p.get("category").get("id"), categoryId));
		}
		if (query != null && !query.isBlank()) {
			String pattern = "%" + query.strip().toLowerCase() + "%";
			spec = spec.and((p, q, cb) -> cb.or(cb.like(cb.lower(p.get("name")), pattern),
					cb.equal(p.get("barcode"), query.strip())));
		}
		if (!includeInactive || sellable) {
			spec = spec.and((p, q, cb) -> cb.isTrue(p.get("active")));
		}
		if (sellable) {
			spec = spec.and((p, q, cb) -> cb.and(cb.greaterThan(p.get("stock"), 0),
					cb.or(cb.isNull(p.get("expiryDate")), cb.greaterThanOrEqualTo(p.get("expiryDate"), today))));
		}
		return spec;
	}

}
