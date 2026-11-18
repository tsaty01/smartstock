package com.smartstock.web;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartstock.auth.User;
import com.smartstock.auth.UserDtos.RegisterRequest;
import com.smartstock.auth.UserRepository;
import com.smartstock.auth.UserService;
import com.smartstock.billing.BillingDtos.CheckoutRequest;
import com.smartstock.billing.BillingDtos.QuoteRequest;
import com.smartstock.billing.BillingDtos.ReceiptView;
import com.smartstock.billing.Order.PaymentMode;
import com.smartstock.billing.OrderService;
import com.smartstock.common.ApiException;
import com.smartstock.inventory.InventoryDtos.ProductView;
import com.smartstock.inventory.ProductFilter;
import com.smartstock.inventory.ProductService;
import com.smartstock.security.CurrentUser;

/** Website till. Totals always come from the shared billing service's quote. */
@Controller
@RequestMapping("/pos")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
class PosPages {

	private static final int RESULTS = 30;

	private final PosCart cart;

	private final ProductService products;

	private final OrderService orders;

	private final UserService userService;

	private final UserRepository users;

	PosPages(PosCart cart, ProductService products, OrderService orders, UserService userService, UserRepository users) {
		this.cart = cart;
		this.products = products;
		this.orders = orders;
		this.userService = userService;
		this.users = users;
	}

	@GetMapping
	String till(@RequestParam(name = "q", required = false) String query,
			@RequestParam(name = "customer", required = false) String customerQuery,
			@AuthenticationPrincipal CurrentUser me, Model model) {
		if (query != null && !query.isBlank()) {
			model.addAttribute("results", products.list(new ProductFilter(null, query, false, true))
				.stream().limit(RESULTS).toList());
		}
		if (customerQuery != null && customerQuery.strip().length() >= 2) {
			model.addAttribute("customers", userService.searchCustomers(customerQuery));
		}
		if (!cart.isEmpty()) {
			try {
				model.addAttribute("quote", orders.quote(new QuoteRequest(cart.items(), cart.getDiscount()), me));
			}
			catch (ApiException ex) {
				model.addAttribute("quoteError", ex.getBody().getDetail());
			}
		}
		model.addAttribute("customer", cart.getCustomerId() == null ? null
				: users.findById(cart.getCustomerId()).map(User::getFullName).orElse(null));
		model.addAttribute("cart", cart);
		model.addAttribute("q", query);
		model.addAttribute("customerQuery", customerQuery);
		model.addAttribute("paymentModes", PaymentMode.values());
		Forms.empty(model, null);
		return "pos";
	}

	/** Adds by product id, or by exact barcode when scanned into the search box. */
	@PostMapping("/add")
	String add(@RequestParam(required = false) Long productId, @RequestParam(required = false) String barcode,
			RedirectAttributes redirect) {
		if (productId == null && barcode != null && !barcode.isBlank()) {
			List<ProductView> match = products.list(new ProductFilter(null, barcode, false, true))
				.stream().filter(p -> barcode.strip().equals(p.barcode())).toList();
			if (match.isEmpty()) {
				redirect.addFlashAttribute(WebSupport.ERROR, "No sellable product has that barcode");
				return "redirect:/pos?q=" + barcode;
			}
			productId = match.getFirst().id();
		}
		if (productId != null) {
			cart.add(productId, 1);
		}
		return "redirect:/pos";
	}

	@PostMapping("/quantity")
	String quantity(@RequestParam long productId, @RequestParam int quantity) {
		cart.set(productId, quantity);
		return "redirect:/pos";
	}

	@PostMapping("/options")
	String options(@RequestParam PaymentMode paymentMode, @RequestParam(required = false) BigDecimal discount,
			@AuthenticationPrincipal CurrentUser me) {
		cart.setPaymentMode(paymentMode);
		if (me.role().isManagement()) {
			cart.setDiscount(discount);
		}
		return "redirect:/pos";
	}

	@PostMapping("/customer")
	String customer(@RequestParam long customerId) {
		cart.setCustomerId(customerId);
		return "redirect:/pos";
	}

	@PostMapping("/customer/new")
	String newCustomer(@Valid @ModelAttribute("form") RegisterRequest form, BindingResult result,
			RedirectAttributes redirect) {
		if (result.hasErrors()) {
			redirect.addFlashAttribute(WebSupport.ERROR, "Check the new customer's details: "
					+ result.getFieldErrors().getFirst().getField() + " " + result.getFieldErrors().getFirst().getDefaultMessage());
			return "redirect:/pos";
		}
		cart.setCustomerId(userService.registerCustomer(form).getId());
		redirect.addFlashAttribute(WebSupport.NOTICE, "Customer registered");
		return "redirect:/pos";
	}

	@PostMapping("/clear")
	String clear() {
		cart.clear();
		return "redirect:/pos";
	}

	@PostMapping("/checkout")
	String checkout(@AuthenticationPrincipal CurrentUser me, RedirectAttributes redirect) {
		if (cart.getCustomerId() == null) {
			redirect.addFlashAttribute(WebSupport.ERROR, "Choose a customer first");
			return "redirect:/pos";
		}
		ReceiptView receipt = orders.checkout(
				new CheckoutRequest(cart.getCustomerId(), cart.items(), cart.getPaymentMode(), cart.getDiscount()), me);
		cart.clear();
		redirect.addFlashAttribute(WebSupport.NOTICE, "Sale completed");
		return "redirect:/orders/" + receipt.id();
	}

}
