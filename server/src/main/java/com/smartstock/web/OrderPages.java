package com.smartstock.web;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartstock.auth.Role;
import com.smartstock.auth.UserDtos.CreateUserRequest;
import com.smartstock.auth.UserDtos.UpdateUserRequest;
import com.smartstock.auth.UserService;
import com.smartstock.billing.BillingDtos.DiscountRuleRequest;
import com.smartstock.billing.DiscountRuleService;
import com.smartstock.billing.OrderService;
import com.smartstock.billing.OrderSummary;
import com.smartstock.common.ApiException;
import com.smartstock.security.CurrentUser;

/** Orders and receipts for everyone, plus user and offer administration. */
@Controller
class OrderPages {

	private static final int PAGE_SIZE = 30;

	private final OrderService orders;

	private final UserService userService;

	private final DiscountRuleService discountRules;

	OrderPages(OrderService orders, UserService userService, DiscountRuleService discountRules) {
		this.orders = orders;
		this.userService = userService;
		this.discountRules = discountRules;
	}

	/** Customers always get only their own orders; the service enforces it. */
	@GetMapping("/orders")
	String orders(@RequestParam(defaultValue = "0") int page, @AuthenticationPrincipal CurrentUser me, Model model) {
		Page<OrderSummary> result = orders.list(null, PageRequest.of(Math.max(page, 0), PAGE_SIZE), me);
		model.addAttribute("orders", result);
		return "orders/list";
	}

	@GetMapping("/orders/{id}")
	String receipt(@PathVariable long id, @AuthenticationPrincipal CurrentUser me, Model model) {
		model.addAttribute("r", orders.receipt(id, me));
		return "orders/receipt";
	}

	@GetMapping("/users")
	@PreAuthorize("hasRole('ADMIN')")
	String users(Model model) {
		model.addAttribute("users", userService.list(null));
		model.addAttribute("roles", Role.values());
		Forms.empty(model, null);
		return "users";
	}

	@PostMapping("/users")
	@PreAuthorize("hasRole('ADMIN')")
	String createUser(@Valid @ModelAttribute("form") CreateUserRequest form, BindingResult result, Model model,
			RedirectAttributes redirect) {
		if (!Forms.check(model, form, result)) {
			model.addAttribute("users", userService.list(null));
			model.addAttribute("roles", Role.values());
			return "users";
		}
		userService.create(form);
		redirect.addFlashAttribute(WebSupport.NOTICE, "User " + form.username() + " added");
		return "redirect:/users";
	}

	@PostMapping("/users/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	String updateUser(@PathVariable long id, @Valid UpdateUserRequest request, BindingResult result,
			@AuthenticationPrincipal CurrentUser me, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			throw ApiException.badRequest("Check the user's details");
		}
		userService.update(id, request, me);
		redirect.addFlashAttribute(WebSupport.NOTICE, "User saved");
		return "redirect:/users";
	}

	@GetMapping("/discounts")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	String discounts(Model model) {
		model.addAttribute("rules", discountRules.list());
		return "discounts";
	}

	@PostMapping("/discounts")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	String createDiscount(@Valid DiscountRuleRequest request, BindingResult result, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			throw ApiException.badRequest("Check the offer: a name, a minimum quantity of 1 and a percentage up to 100");
		}
		discountRules.create(request);
		redirect.addFlashAttribute(WebSupport.NOTICE, "Offer added");
		return "redirect:/discounts";
	}

	@PostMapping("/discounts/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	String updateDiscount(@PathVariable long id, @Valid DiscountRuleRequest request, BindingResult result,
			RedirectAttributes redirect) {
		if (result.hasErrors()) {
			throw ApiException.badRequest("Check the offer: a name, a minimum quantity of 1 and a percentage up to 100");
		}
		discountRules.update(id, request);
		redirect.addFlashAttribute(WebSupport.NOTICE, "Offer saved");
		return "redirect:/discounts";
	}

}
