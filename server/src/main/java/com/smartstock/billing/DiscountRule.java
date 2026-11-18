package com.smartstock.billing;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Automatic basket discount: the highest matching percentage applies when no manual discount is given. */
@Entity
@Table(name = "discount_rules")
public class DiscountRule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	private int minQuantity;

	private BigDecimal minAmount;

	private BigDecimal percent;

	private boolean active;

	private LocalDate validFrom;

	private LocalDate validTo;

	protected DiscountRule() {
	}

	public DiscountRule(String name) {
		this.name = name;
	}

	public void update(String name, int minQuantity, BigDecimal minAmount, BigDecimal percent, boolean active,
			LocalDate validFrom, LocalDate validTo) {
		this.name = name;
		this.minQuantity = minQuantity;
		this.minAmount = minAmount;
		this.percent = percent;
		this.active = active;
		this.validFrom = validFrom;
		this.validTo = validTo;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getMinQuantity() {
		return minQuantity;
	}

	public BigDecimal getMinAmount() {
		return minAmount;
	}

	public BigDecimal getPercent() {
		return percent;
	}

	public boolean isActive() {
		return active;
	}

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public LocalDate getValidTo() {
		return validTo;
	}

}
