package com.smartstock.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.smartstock.ApiTestSupport;
import com.smartstock.auth.UserRepository;
import com.smartstock.billing.BillingDtos.CartItem;
import com.smartstock.billing.BillingDtos.CheckoutRequest;
import com.smartstock.billing.Order.PaymentMode;
import com.smartstock.common.ApiException;
import com.smartstock.security.CurrentUser;

class CheckoutTests extends ApiTestSupport {

	@Autowired
	OrderService orderService;

	@Autowired
	UserRepository users;

	String manager;

	String cashier;

	long customer;

	@BeforeEach
	void setUp() throws Exception {
		manager = login("manager");
		cashier = login("cashier");
		customer = userId(cashier, "customer");
	}

	@Test
	void saleUpdatesOrderItemsStockAndMovementsTogether() throws Exception {
		long product = createProduct(manager, "100.00", 5);

		ResultActions sale = post(cashier, "/api/orders", cart(customer, "null", product, 2))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.items[0].quantity").value(2))
			.andExpect(jsonPath("$.customerName").value("Chitra Customer"))
			.andExpect(jsonPath("$.cashierName").value("Chetan Cashier"));
		assertThat(decimal(sale, "$.subtotal")).isEqualByComparingTo("200.00");
		assertThat(decimal(sale, "$.discount")).isEqualByComparingTo("0");
		assertThat(decimal(sale, "$.tax")).isEqualByComparingTo("36.00");
		assertThat(decimal(sale, "$.total")).isEqualByComparingTo("236.00");

		assertThat(stock(manager, product)).isEqualTo(3);
		get(manager, "/api/stock-movements?productId=" + product).andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].reason").value("SALE"))
			.andExpect(jsonPath("$.content[0].change").value(-2))
			.andExpect(jsonPath("$.content[0].orderId").value(id(sale)))
			.andExpect(jsonPath("$.content[1].reason").value("INITIAL"));
	}

	@Test
	void bestAutomaticDiscountApplies() throws Exception {
		long product = createProduct(manager, "600.00", 5);
		ResultActions sale = post(cashier, "/api/orders", cart(customer, "null", product, 1))
			.andExpect(status().isCreated());
		assertThat(decimal(sale, "$.discount")).isEqualByComparingTo("60.00");
		assertThat(decimal(sale, "$.tax")).isEqualByComparingTo("97.20");
		assertThat(decimal(sale, "$.total")).isEqualByComparingTo("637.20");
	}

	@Test
	void quoteMatchesCheckoutWithoutChangingStock() throws Exception {
		long product = createProduct(manager, "1200.00", 3);
		ResultActions quote = post(cashier, "/api/orders/quote", """
				{"items": [{"productId": %d, "quantity": 1}]}""".formatted(product)).andExpect(status().isOk())
			.andExpect(jsonPath("$.discountSource").value("RULE"));
		assertThat(decimal(quote, "$.total")).isEqualByComparingTo("1203.60");
		assertThat(stock(manager, product)).isEqualTo(3);
	}

	@Test
	void repeatedLinesAreCombinedBeforeTheStockCheck() throws Exception {
		long product = createProduct(manager, "10.00", 1);
		post(cashier, "/api/orders", cart(customer, "null", product, 1, product, 1))
			.andExpect(status().isUnprocessableContent());
		assertThat(stock(manager, product)).isEqualTo(1);
	}

	@Test
	void rejectedSaleLeavesNoPartialChanges() throws Exception {
		long available = createProduct(manager, "10.00", 5);
		long scarce = createProduct(manager, "10.00", 1);
		post(cashier, "/api/orders", cart(customer, "null", available, 2, scarce, 3))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Insufficient stock")));
		assertThat(stock(manager, available)).isEqualTo(5);
		get(manager, "/api/stock-movements?productId=" + available).andExpect(jsonPath("$.content.length()").value(1));
	}

	@Test
	void expiredInactiveAndUnknownProductsCannotBeSold() throws Exception {
		long expired = createProduct(manager, "10.00", 5, 0, LocalDate.now().minusDays(2).toString(), true);
		long inactive = createProduct(manager, "10.00", 5, 0, null, false);
		post(cashier, "/api/orders", cart(customer, "null", expired, 1)).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("expired")));
		post(cashier, "/api/orders", cart(customer, "null", inactive, 1)).andExpect(status().isUnprocessableContent());
		post(cashier, "/api/orders", cart(customer, "null", Long.MAX_VALUE, 1)).andExpect(status().isNotFound());
	}

	@Test
	void saleNeedsAnActiveCustomer() throws Exception {
		long product = createProduct(manager, "10.00", 5);
		long staffMember = users.findByUsernameIgnoreCase("manager").orElseThrow().getId();
		post(cashier, "/api/orders", cart(staffMember, "null", product, 1)).andExpect(status().isUnprocessableContent());
	}

	@Test
	void onlyManagersSetManualDiscountsWithinTheSubtotal() throws Exception {
		long product = createProduct(manager, "100.00", 5);
		post(cashier, "/api/orders", cart(customer, "5.00", product, 1)).andExpect(status().isForbidden());
		post(manager, "/api/orders", cart(customer, "150.00", product, 1)).andExpect(status().isUnprocessableContent());
		ResultActions sale = post(manager, "/api/orders", cart(customer, "20.00", product, 1))
			.andExpect(status().isCreated());
		assertThat(decimal(sale, "$.total")).isEqualByComparingTo("94.40");
	}

	@Test
	void concurrentCheckoutsOfTheLastUnitSellItOnce() throws Exception {
		long product = createProduct(manager, "10.00", 1);
		CurrentUser first = CurrentUser.of(users.findByUsernameIgnoreCase("cashier").orElseThrow());
		CurrentUser second = CurrentUser.of(users.findByUsernameIgnoreCase("cashier2").orElseThrow());
		CheckoutRequest request = new CheckoutRequest(customer, List.of(new CartItem(product, 1)), PaymentMode.CASH,
				null);

		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> results = new ArrayList<>();
		try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
			for (CurrentUser cashierUser : List.of(first, second)) {
				Callable<Boolean> attempt = () -> {
					start.await();
					try {
						orderService.checkout(request, cashierUser);
						return true;
					}
					catch (ApiException ex) {
						return false;
					}
				};
				results.add(pool.submit(attempt));
			}
			start.countDown();
		}

		long successes = results.stream().filter(f -> f.resultNow()).count();
		assertThat(successes).isEqualTo(1);
		assertThat(stock(manager, product)).isZero();
		get(manager, "/api/stock-movements?productId=" + product).andExpect(jsonPath("$.content.length()").value(2));
	}

}
