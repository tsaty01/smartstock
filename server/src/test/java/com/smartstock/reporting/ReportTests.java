package com.smartstock.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.smartstock.ApiTestSupport;

class ReportTests extends ApiTestSupport {

	@Test
	void dashboardReflectsTodaysSales() throws Exception {
		String manager = login("manager");
		String cashier = login("cashier");
		BigDecimal before = decimal(get(manager, "/api/reports/dashboard").andExpect(status().isOk()),
				"$.today.revenue");

		long product = createProduct(manager, "50.00", 3);
		ResultActions sale = post(cashier, "/api/orders", cart(userId(cashier, "customer"), "null", product, 1))
			.andExpect(status().isCreated());

		ResultActions dashboard = get(manager, "/api/reports/dashboard").andExpect(status().isOk())
			.andExpect(jsonPath("$.monthlySales").isNotEmpty())
			.andExpect(jsonPath("$.lowStock").isArray())
			.andExpect(jsonPath("$.expired").isArray());
		assertThat(decimal(dashboard, "$.today.revenue")).isEqualByComparingTo(before.add(decimal(sale, "$.total")));
	}

	@Test
	void salesReportsCoverProductsAndCategories() throws Exception {
		String manager = login("manager");
		String cashier = login("cashier");
		long product = createProduct(manager, "25.00", 4);
		post(cashier, "/api/orders", cart(userId(cashier, "customer"), "null", product, 2))
			.andExpect(status().isCreated());

		get(manager, "/api/reports/products").andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.id == %d)].units".formatted(product)).value(2));
		get(manager, "/api/reports/categories?from=2020-01-01").andExpect(status().isOk())
			.andExpect(jsonPath("$[0].revenue").isNumber());
		get(manager, "/api/reports/clearance?days=30").andExpect(status().isOk());
		get(manager, "/api/reports/products?from=2026-02-01&to=2026-01-01").andExpect(status().isBadRequest());
	}

	@Test
	void reportsAreForManagementOnly() throws Exception {
		get(login("cashier"), "/api/reports/dashboard").andExpect(status().isForbidden());
	}

}
