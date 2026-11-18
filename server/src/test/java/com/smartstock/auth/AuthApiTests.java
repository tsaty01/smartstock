package com.smartstock.auth;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.smartstock.ApiTestSupport;

class AuthApiTests extends ApiTestSupport {

	@Test
	void loginReturnsTokenForCurrentProfile() throws Exception {
		String token = login("admin");
		get(token, "/api/auth/me").andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("admin"))
			.andExpect(jsonPath("$.role").value("ADMIN"))
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void wrongPasswordAndUnknownUserAreRejectedAlike() throws Exception {
		post(null, "/api/auth/login", """
				{"username": "admin", "password": "wrong-password"}""").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Invalid username or password"));
		post(null, "/api/auth/login", """
				{"username": "nobody", "password": "wrong-password"}""").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Invalid username or password"));
	}

	@Test
	void apiRequiresAToken() throws Exception {
		get(null, "/api/products").andExpect(status().isUnauthorized());
		get("not-a-token", "/api/products").andExpect(status().isUnauthorized());
	}

	@Test
	void registeredCustomerGetsCustomerAccessOnly() throws Exception {
		String username = "c" + UUID.randomUUID().toString().substring(0, 8);
		String token = read(post(null, "/api/auth/register", """
				{"username": "%s", "password": "long-enough", "fullName": "New Customer", "phone": "9123456789"}"""
			.formatted(username)).andExpect(status().isCreated()).andExpect(jsonPath("$.user.role").value("CUSTOMER")),
				"$.token");

		get(token, "/api/products").andExpect(status().isOk());
		get(token, "/api/users").andExpect(status().isForbidden());
		get(token, "/api/reports/dashboard").andExpect(status().isForbidden());
		get(token, "/api/notifications").andExpect(status().isForbidden());
		post(token, "/api/orders", cart(1, "null", 1, 1)).andExpect(status().isForbidden());
	}

	@Test
	void registrationValidatesInput() throws Exception {
		post(null, "/api/auth/register", """
				{"username": "x", "password": "short", "fullName": "", "phone": "123"}""")
			.andExpect(status().isBadRequest());
		post(null, "/api/auth/register", """
				{"username": "admin", "password": "long-enough", "fullName": "Duplicate"}""")
			.andExpect(status().isConflict());
	}

	@Test
	void disablingAnAccountRevokesItsTokensImmediately() throws Exception {
		String admin = login("admin");
		String username = "k" + UUID.randomUUID().toString().substring(0, 8);
		long id = id(post(admin, "/api/users", """
				{"username": "%s", "password": "%s", "role": "CASHIER", "fullName": "Temp Cashier"}"""
			.formatted(username, DEMO_PASSWORD)).andExpect(status().isCreated()));
		String cashier = login(username);
		get(cashier, "/api/products").andExpect(status().isOk());

		put(admin, "/api/users/" + id, """
				{"role": "CASHIER", "fullName": "Temp Cashier", "active": false}""").andExpect(status().isOk());

		get(cashier, "/api/products").andExpect(status().isUnauthorized());
		post(null, "/api/auth/login", """
				{"username": "%s", "password": "%s"}""".formatted(username, DEMO_PASSWORD))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminCannotLockThemselvesOut() throws Exception {
		String admin = login("admin");
		long self = ((Number) read(get(admin, "/api/auth/me"), "$.id")).longValue();
		put(admin, "/api/users/" + self, """
				{"role": "ADMIN", "fullName": "Asha Admin", "active": false}""")
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	void cashierRegistersCustomerWithoutLosingSession() throws Exception {
		String cashier = login("cashier");
		String username = "w" + UUID.randomUUID().toString().substring(0, 8);
		post(cashier, "/api/customers", """
				{"username": "%s", "password": "long-enough", "fullName": "Walk In"}""".formatted(username))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.role").value("CUSTOMER"));
		get(cashier, "/api/auth/me").andExpect(jsonPath("$.username").value("cashier"));
	}

}
