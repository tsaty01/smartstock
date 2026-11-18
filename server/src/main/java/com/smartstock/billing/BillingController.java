package com.smartstock.billing;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.billing.BillingDtos.CheckoutRequest;
import com.smartstock.billing.BillingDtos.DiscountRuleRequest;
import com.smartstock.billing.BillingDtos.DiscountRuleView;
import com.smartstock.billing.BillingDtos.QuoteRequest;
import com.smartstock.billing.BillingDtos.QuoteView;
import com.smartstock.billing.BillingDtos.ReceiptView;
import com.smartstock.security.CurrentUser;

@RestController
@RequestMapping("/api")
class BillingController {

	private static final String STAFF = "hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')";

	private static final String MANAGEMENT = "hasAnyRole('ADMIN', 'MANAGER')";

	private final OrderService orders;

	private final DiscountRuleService discountRules;

	BillingController(OrderService orders, DiscountRuleService discountRules) {
		this.orders = orders;
		this.discountRules = discountRules;
	}

	@PostMapping("/orders/quote")
	@PreAuthorize(STAFF)
	QuoteView quote(@Valid @RequestBody QuoteRequest request, @AuthenticationPrincipal CurrentUser user) {
		return orders.quote(request, user);
	}

	@PostMapping("/orders")
	@PreAuthorize(STAFF)
	@ResponseStatus(HttpStatus.CREATED)
	ReceiptView checkout(@Valid @RequestBody CheckoutRequest request, @AuthenticationPrincipal CurrentUser user) {
		return orders.checkout(request, user);
	}

	@GetMapping("/orders")
	PagedModel<OrderSummary> orders(@RequestParam(required = false) Long customerId, Pageable pageable,
			@AuthenticationPrincipal CurrentUser user) {
		return new PagedModel<>(orders.list(customerId, pageable, user));
	}

	@GetMapping("/orders/{id}")
	ReceiptView receipt(@PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
		return orders.receipt(id, user);
	}

	@GetMapping("/discount-rules")
	@PreAuthorize(STAFF)
	List<DiscountRuleView> discountRules() {
		return discountRules.list();
	}

	@PostMapping("/discount-rules")
	@PreAuthorize(MANAGEMENT)
	@ResponseStatus(HttpStatus.CREATED)
	DiscountRuleView createDiscountRule(@Valid @RequestBody DiscountRuleRequest request) {
		return discountRules.create(request);
	}

	@PutMapping("/discount-rules/{id}")
	@PreAuthorize(MANAGEMENT)
	DiscountRuleView updateDiscountRule(@PathVariable long id, @Valid @RequestBody DiscountRuleRequest request) {
		return discountRules.update(id, request);
	}

}
