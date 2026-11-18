package com.smartstock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Integration tests against PostgreSQL with the demo accounts (password "smartstock"). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "smartstock.security.token-secret=test-only-token-secret-0123456789abcdef")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
public abstract class ApiTestSupport {

	protected static final String DEMO_PASSWORD = "smartstock";

	@Autowired
	protected MockMvc mvc;

	@DynamicPropertySource
	static void storage(DynamicPropertyRegistry registry) throws IOException {
		Path storage = Files.createTempDirectory("smartstock-test");
		registry.add("smartstock.storage-dir", storage::toString);
	}

	protected String login(String username) throws Exception {
		String body = """
				{"username": "%s", "password": "%s"}""".formatted(username, DEMO_PASSWORD);
		return read(call(HttpMethod.POST, null, "/api/auth/login", body).andExpect(status().isOk()), "$.token");
	}

	protected ResultActions call(HttpMethod method, String token, String url, String json) throws Exception {
		MockHttpServletRequestBuilder builder = request(method, url);
		if (token != null) {
			builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		}
		if (json != null) {
			builder.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return mvc.perform(builder);
	}

	protected ResultActions get(String token, String url) throws Exception {
		return call(HttpMethod.GET, token, url, null);
	}

	protected ResultActions post(String token, String url, String json) throws Exception {
		return call(HttpMethod.POST, token, url, json);
	}

	protected ResultActions put(String token, String url, String json) throws Exception {
		return call(HttpMethod.PUT, token, url, json);
	}

	protected static <T> T read(ResultActions result, String path) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
	}

	protected static BigDecimal decimal(ResultActions result, String path) throws Exception {
		Object value = read(result, path);
		return new BigDecimal(value.toString());
	}

	protected static long id(ResultActions result) throws Exception {
		return ((Number) read(result, "$.id")).longValue();
	}

	protected long userId(String token, String username) throws Exception {
		List<Integer> ids = read(get(token, "/api/customers?q=" + username).andExpect(status().isOk()),
				"$[?(@.username == '%s')].id".formatted(username));
		return ids.getFirst();
	}

	protected long firstCategoryId(String token) throws Exception {
		return ((Number) read(get(token, "/api/categories"), "$[0].id")).longValue();
	}

	/** Creates an active product owned by the test, so tests do not share stock. */
	protected long createProduct(String managerToken, String price, int stock, int reorderLevel, String expiryDate,
			boolean active) throws Exception {
		String body = """
				{"categoryId": %d, "name": "Test %s", "price": %s, "reorderLevel": %d, "expiryDate": %s,
				 "active": %s, "initialStock": %d}""".formatted(firstCategoryId(managerToken),
				UUID.randomUUID(), price, reorderLevel, expiryDate == null ? "null" : "\"" + expiryDate + "\"",
				active, stock);
		return id(post(managerToken, "/api/products", body).andExpect(status().isCreated()));
	}

	protected long createProduct(String managerToken, String price, int stock) throws Exception {
		return createProduct(managerToken, price, stock, 0, null, true);
	}

	protected int stock(String token, long productId) throws Exception {
		return read(get(token, "/api/products/" + productId).andExpect(status().isOk()), "$.stock");
	}

	protected static String cart(long customerId, String discount, long... productQuantityPairs) {
		StringBuilder items = new StringBuilder();
		for (int i = 0; i < productQuantityPairs.length; i += 2) {
			items.append(i == 0 ? "" : ",")
				.append("{\"productId\": %d, \"quantity\": %d}".formatted(productQuantityPairs[i],
						productQuantityPairs[i + 1]));
		}
		return """
				{"customerId": %d, "paymentMode": "UPI", "discount": %s, "items": [%s]}""".formatted(customerId,
				discount, items);
	}

}
