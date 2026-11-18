package com.smartstock.billing;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.smartstock.inventory.Product;

/** Line of a sale; name and price are snapshots taken at the time of sale. */
@Entity
@Table(name = "order_items")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Product product;

	private String productName;

	private int quantity;

	private BigDecimal unitPrice;

	private BigDecimal lineTotal;

	protected OrderItem() {
	}

	OrderItem(Order order, Bill.Line line) {
		this.order = order;
		this.product = line.product();
		this.productName = line.product().getName();
		this.quantity = line.quantity();
		this.unitPrice = line.unitPrice();
		this.lineTotal = line.lineTotal();
	}

	public Long getProductId() {
		return product.getId();
	}

	public String getProductName() {
		return productName;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public BigDecimal getLineTotal() {
		return lineTotal;
	}

}
