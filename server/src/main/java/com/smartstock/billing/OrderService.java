package com.smartstock.billing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.auth.Role;
import com.smartstock.auth.User;
import com.smartstock.auth.UserRepository;
import com.smartstock.billing.BillingDtos.CartItem;
import com.smartstock.billing.BillingDtos.CheckoutRequest;
import com.smartstock.billing.BillingDtos.QuoteRequest;
import com.smartstock.billing.BillingDtos.QuoteView;
import com.smartstock.billing.BillingDtos.ReceiptView;
import com.smartstock.common.ApiException;
import com.smartstock.inventory.Product;
import com.smartstock.inventory.ProductRepository;
import com.smartstock.inventory.ProductService;
import com.smartstock.security.CurrentUser;

@Service
public class OrderService {

	private final OrderRepository orders;

	private final UserRepository users;

	private final ProductRepository products;

	private final ProductService productService;

	private final BillCalculator calculator;

	OrderService(OrderRepository orders, UserRepository users, ProductRepository products,
			ProductService productService, BillCalculator calculator) {
		this.orders = orders;
		this.users = users;
		this.products = products;
		this.productService = productService;
		this.calculator = calculator;
	}

	/** Prices a cart without reserving stock, for live totals at the till. */
	@Transactional(readOnly = true)
	public QuoteView quote(QuoteRequest request, CurrentUser actor) {
		requireDiscountPermission(request.discount(), actor);
		Map<Long, Integer> quantities = merge(request.items());
		List<Product> found = products.findAllByIdIn(quantities.keySet());
		if (found.size() != quantities.size()) {
			throw ApiException.notFound("Product");
		}
		return QuoteView.of(calculator.price(found, quantities, request.discount()));
	}

	/**
	 * Completes a sale atomically: products are locked, validated and priced, then the order, its item
	 * snapshots, stock decrements and movements are written together or not at all.
	 */
	@Transactional
	public ReceiptView checkout(CheckoutRequest request, CurrentUser actor) {
		requireDiscountPermission(request.discount(), actor);
		User customer = users.findById(request.customerId())
			.filter(u -> u.getRole() == Role.CUSTOMER && u.isActive())
			.orElseThrow(() -> ApiException.rejected("Customer not found"));
		Map<Long, Integer> quantities = merge(request.items());
		List<Product> locked = productService.lockForSale(List.copyOf(quantities.keySet()));
		Bill bill = calculator.price(locked, quantities, request.discount());

		Order order = orders.save(new Order(customer, users.getReferenceById(actor.id()), request.paymentMode(), bill));
		for (Bill.Line line : bill.lines()) {
			productService.recordSale(line.product(), line.quantity(), order.getId(), actor);
		}
		return ReceiptView.of(orders.findWithDetailsById(order.getId()).orElseThrow());
	}

	/** Customers see only their own orders; staff see all, optionally for one customer. */
	@Transactional(readOnly = true)
	public Page<OrderSummary> list(Long customerId, Pageable pageable, CurrentUser actor) {
		if (actor.role() == Role.CUSTOMER) {
			return orders.findSummariesByCustomer(actor.id(), pageable);
		}
		return customerId == null ? orders.findSummaries(pageable)
				: orders.findSummariesByCustomer(customerId, pageable);
	}

	/** Another customer's order is reported as missing, so order ids cannot be probed. */
	@Transactional(readOnly = true)
	public ReceiptView receipt(long id, CurrentUser actor) {
		return orders.findWithDetailsById(id)
			.filter(o -> actor.role().isStaff() || o.getCustomer().getId() == actor.id())
			.map(ReceiptView::of)
			.orElseThrow(() -> ApiException.notFound("Order"));
	}

	private static void requireDiscountPermission(BigDecimal discount, CurrentUser actor) {
		if (discount != null && !actor.role().isManagement()) {
			throw ApiException.forbidden("Only managers can apply a manual discount");
		}
	}

	/** Combines repeated lines for the same product, ordered by product id. */
	private static Map<Long, Integer> merge(List<CartItem> items) {
		Map<Long, Integer> quantities = new TreeMap<>();
		items.forEach(item -> quantities.merge(item.productId(), item.quantity(), Integer::sum));
		return quantities;
	}

}
