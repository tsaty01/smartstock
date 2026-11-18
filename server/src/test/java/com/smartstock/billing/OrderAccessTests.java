package com.smartstock.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.smartstock.ApiTestSupport;

class OrderAccessTests extends ApiTestSupport {

	@Test
	void customersSeeOnlyTheirOwnOrders() throws Exception {
		String manager = login("manager");
		String cashier = login("cashier");
		long product = createProduct(manager, "10.00", 10);
		long own = id(post(cashier, "/api/orders", cart(userId(cashier, "customer"), "null", product, 1))
			.andExpect(status().isCreated()));
		long other = id(post(cashier, "/api/orders", cart(userId(cashier, "customer2"), "null", product, 1))
			.andExpect(status().isCreated()));

		String customer = login("customer");
		get(customer, "/api/orders/" + own).andExpect(status().isOk());
		get(customer, "/api/orders/" + other).andExpect(status().isNotFound());

		long otherCustomerId = userId(cashier, "customer2");
		List<String> names = read(get(customer, "/api/orders?size=100&customerId=" + otherCustomerId)
			.andExpect(status().isOk()), "$.content[*].customerName");
		assertThat(names).isNotEmpty().containsOnly("Chitra Customer");

		get(cashier, "/api/orders/" + other).andExpect(status().isOk());
		List<Integer> ids = read(get(manager, "/api/orders?size=100").andExpect(status().isOk()), "$.content[*].id");
		assertThat(ids).contains((int) own, (int) other);
	}

}
