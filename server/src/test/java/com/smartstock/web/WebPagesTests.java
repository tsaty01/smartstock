package com.smartstock.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.smartstock.ApiTestSupport;

class WebPagesTests extends ApiTestSupport {

	private static MockHttpServletRequestBuilder page(String url) {
		return MockMvcRequestBuilders.get(url);
	}

	private static MockHttpServletRequestBuilder form(String url) {
		return MockMvcRequestBuilders.post(url);
	}

	private MockHttpSession signIn(String username) throws Exception {
		MvcResult result = mvc.perform(formLogin("/login").user(username).password(DEMO_PASSWORD))
			.andExpect(redirectedUrl("/"))
			.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	@Test
	void eachRoleLandsOnItsHomePage() throws Exception {
		mvc.perform(page("/").session(signIn("manager"))).andExpect(redirectedUrl("/dashboard"));
		mvc.perform(page("/").session(signIn("cashier"))).andExpect(redirectedUrl("/pos"));
		mvc.perform(page("/").session(signIn("customer"))).andExpect(redirectedUrl("/orders"));
	}

	@Test
	void wrongPasswordReturnsToLogin() throws Exception {
		mvc.perform(formLogin("/login").user("manager").password("wrong")).andExpect(redirectedUrl("/login?error"));
	}

	@Test
	void pagesAreLimitedByRole() throws Exception {
		MockHttpSession manager = signIn("manager");
		mvc.perform(page("/dashboard").session(manager)).andExpect(status().isOk())
			.andExpect(content().string(containsString("Monthly revenue")));
		mvc.perform(page("/products").session(manager)).andExpect(status().isOk());
		mvc.perform(page("/users").session(manager)).andExpect(status().isForbidden());

		MockHttpSession cashier = signIn("cashier");
		mvc.perform(page("/dashboard").session(cashier)).andExpect(status().isForbidden());
		mvc.perform(page("/pos").session(cashier)).andExpect(status().isOk());

		MockHttpSession customer = signIn("customer");
		mvc.perform(page("/pos").session(customer)).andExpect(status().isForbidden());
		mvc.perform(page("/shop").session(customer)).andExpect(status().isOk());
	}

	/** Every rendered post form must carry the CSRF token, or the browser gets 403 on submit. */
	@Test
	void everyPostFormCarriesTheCsrfToken() throws Exception {
		assertEveryFormHasToken(mvc.perform(page("/login")).andReturn().getResponse().getContentAsString());
		assertEveryFormHasToken(mvc.perform(page("/register")).andReturn().getResponse().getContentAsString());
		MockHttpSession admin = signIn("admin");
		long product = createProduct(login("manager"), "5.00", 1);
		mvc.perform(form("/pos/add").param("productId", String.valueOf(product)).session(admin).with(csrf()));
		for (String url : new String[] { "/dashboard", "/products", "/products/" + product, "/products/new",
				"/categories", "/pos?q=a&customer=cu", "/orders", "/alerts", "/reports", "/discounts", "/users" }) {
			String html = mvc.perform(page(url).session(admin)).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
			assertEveryFormHasToken(html);
		}
	}

	private static void assertEveryFormHasToken(String html) {
		int forms = html.split("method=\"post\"", -1).length - 1;
		int tokens = html.split("name=\"_csrf\"", -1).length - 1;
		assertThat(forms).isPositive();
		assertThat(tokens).isEqualTo(forms);
	}

	@Test
	void formsRequireTheCsrfToken() throws Exception {
		MockHttpSession cashier = signIn("cashier");
		mvc.perform(form("/pos/clear").session(cashier)).andExpect(status().isForbidden());
		mvc.perform(form("/pos/clear").session(cashier).with(csrf())).andExpect(redirectedUrl("/pos"));
	}

	@Test
	void tillSaleProducesAReceiptVisibleToTheCustomer() throws Exception {
		String apiManager = login("manager");
		long product = createProduct(apiManager, "40.00", 5);
		long customer = userId(apiManager, "customer2");

		MockHttpSession cashier = signIn("cashier");
		mvc.perform(form("/pos/add").param("productId", String.valueOf(product)).session(cashier).with(csrf()));
		mvc.perform(form("/pos/quantity").param("productId", String.valueOf(product)).param("quantity", "2")
			.session(cashier).with(csrf()));
		mvc.perform(form("/pos/customer").param("customerId", String.valueOf(customer)).session(cashier).with(csrf()));
		mvc.perform(page("/pos").session(cashier)).andExpect(status().isOk())
			.andExpect(content().string(containsString("Charge ₹94.40")));

		String receipt = mvc.perform(form("/pos/checkout").session(cashier).with(csrf()))
			.andExpect(redirectedUrlPattern("/orders/*"))
			.andReturn().getResponse().getRedirectedUrl();
		assertThat(stock(apiManager, product)).isEqualTo(3);

		mvc.perform(page(receipt).session(signIn("customer2"))).andExpect(status().isOk())
			.andExpect(content().string(containsString("₹94.40")));
		mvc.perform(page(receipt).session(signIn("customer"))).andExpect(status().isNotFound());
	}

	@Test
	void customersRegisterThemselvesAndAtTheTill() throws Exception {
		String self = "s" + UUID.randomUUID().toString().substring(0, 8);
		mvc.perform(form("/register").with(csrf()).param("username", self).param("password", DEMO_PASSWORD)
			.param("fullName", "Self Registered").param("phone", "")).andExpect(redirectedUrl("/login?registered"));
		signIn(self);

		String walkIn = "t" + UUID.randomUUID().toString().substring(0, 8);
		MockHttpSession cashier = signIn("cashier");
		mvc.perform(form("/pos/customer/new").session(cashier).with(csrf()).param("username", walkIn)
			.param("password", "long-enough").param("fullName", "Walk In")).andExpect(redirectedUrl("/pos"));
		mvc.perform(page("/pos").session(cashier))
			.andExpect(content().string(containsString("Walk In")));
	}

	@Test
	void disablingAUserEndsTheirBrowserSession() throws Exception {
		String apiAdmin = login("admin");
		String username = "d" + UUID.randomUUID().toString().substring(0, 8);
		long id = id(post(apiAdmin, "/api/users", """
				{"username": "%s", "password": "%s", "role": "CASHIER", "fullName": "Doomed"}"""
			.formatted(username, DEMO_PASSWORD)));
		MockHttpSession session = signIn(username);
		mvc.perform(page("/pos").session(session)).andExpect(status().isOk());

		put(apiAdmin, "/api/users/" + id, """
				{"role": "CASHIER", "fullName": "Doomed", "active": false}""");
		mvc.perform(page("/pos").session(session)).andExpect(redirectedUrl("/login?disabled"));
	}

	@Test
	void managerEditsAProductThroughTheForm() throws Exception {
		String apiManager = login("manager");
		long product = createProduct(apiManager, "10.00", 4);
		MockHttpSession manager = signIn("manager");
		mvc.perform(form("/products/" + product + "/edit").session(manager).with(csrf())
			.param("categoryId", String.valueOf(firstCategoryId(apiManager))).param("name", "Edited in browser")
			.param("price", "12.50").param("reorderLevel", "2").param("barcode", "").param("expiryDate", "")
			.param("active", "true")).andExpect(redirectedUrl("/products/" + product));
		get(apiManager, "/api/products/" + product)
			.andExpect(jsonPath("$.name")
				.value("Edited in browser"))
			.andExpect(jsonPath("$.stock").value(4));
	}

}
