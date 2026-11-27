package com.smartstock.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import com.smartstock.ApiTestSupport;
import com.smartstock.common.SmartstockProperties;

class InventoryTests extends ApiTestSupport {

	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0 };

	String manager;

	@Autowired ProductImageService images;
	@Autowired TransactionTemplate transactions;
	@Autowired SmartstockProperties properties;

	@BeforeEach
	void setUp() throws Exception {
		manager = login("manager");
	}

	@Test
	void restockAddsStockAndRecordsWhoDidIt() throws Exception {
		long product = createProduct(manager, "10.00", 2);
		post(manager, "/api/products/" + product + "/restock", """
				{"quantity": 8, "notes": "Supplier invoice 42"}""").andExpect(status().isOk())
			.andExpect(jsonPath("$.stock").value(10));
		get(manager, "/api/stock-movements?productId=" + product)
			.andExpect(jsonPath("$.content[0].reason").value("RESTOCK"))
			.andExpect(jsonPath("$.content[0].change").value(8))
			.andExpect(jsonPath("$.content[0].performedBy").value("Manav Manager"))
			.andExpect(jsonPath("$.content[0].notes").value("Supplier invoice 42"));
	}

	@Test
	void adjustmentsCannotMakeStockNegative() throws Exception {
		long product = createProduct(manager, "10.00", 2);
		post(manager, "/api/products/" + product + "/adjustments", """
				{"change": -3, "reason": "ADJUSTMENT", "notes": "Count"}""")
			.andExpect(status().isUnprocessableContent());
		post(manager, "/api/products/" + product + "/adjustments", """
				{"change": -2, "reason": "EXPIRED_REMOVAL", "notes": "Expired batch"}""").andExpect(status().isOk())
			.andExpect(jsonPath("$.stock").value(0));
		post(manager, "/api/products/" + product + "/adjustments", """
				{"change": 1, "reason": "SALE", "notes": "Not allowed"}""").andExpect(status().isBadRequest());
	}

	@Test
	void crossingTheReorderLevelRaisesOneAlert() throws Exception {
		long product = createProduct(manager, "10.00", 5, 3, null, true);
		String cashier = login("cashier");
		long customer = userId(cashier, "customer");
		post(cashier, "/api/orders", cart(customer, "null", product, 2)).andExpect(status().isCreated());
		post(cashier, "/api/orders", cart(customer, "null", product, 1)).andExpect(status().isCreated());

		List<Integer> alerts = read(get(manager, "/api/notifications"),
				"$[?(@.productId == %d && @.type == 'LOW_STOCK')].id".formatted(product));
		assertThat(alerts).hasSize(1);
		get(manager, "/api/products/" + product).andExpect(jsonPath("$.lowStock").value(true));
	}

	@Test
	void expiryWithinTheWarningWindowRaisesAnAlert() throws Exception {
		long product = createProduct(manager, "10.00", 5, 0, LocalDate.now().plusDays(10).toString(), true);
		List<Integer> alerts = read(get(manager, "/api/notifications?unread=true"),
				"$[?(@.productId == %d && @.type == 'EXPIRY')].id".formatted(product));
		assertThat(alerts).hasSize(1);
		post(manager, "/api/notifications/" + alerts.getFirst() + "/read", null).andExpect(status().isNoContent());
	}

	@Test
	void replacingAProductKeepsStockAndRejectsDuplicateBarcodes() throws Exception {
		long category = firstCategoryId(manager);
		long product = createProduct(manager, "10.00", 7);
		String body = """
				{"categoryId": %d, "name": "Renamed", "description": "New text", "barcode": "%s", "price": 12.50,
				 "reorderLevel": 2, "active": true}""";
		put(manager, "/api/products/" + product, body.formatted(category, "TEST-" + product))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Renamed"))
			.andExpect(jsonPath("$.stock").value(7));
		long other = createProduct(manager, "10.00", 1);
		put(manager, "/api/products/" + other, body.formatted(category, "TEST-" + product))
			.andExpect(status().isConflict());
	}

	@Test
	void staffOutsideManagementCannotChangeTheCatalog() throws Exception {
		String cashier = login("cashier");
		long product = createProduct(manager, "10.00", 1);
		post(cashier, "/api/products/" + product + "/restock", """
				{"quantity": 1}""").andExpect(status().isForbidden());
		post(cashier, "/api/categories", """
				{"name": "Nope"}""").andExpect(status().isForbidden());
		get(cashier, "/api/products?sellable=true").andExpect(status().isOk());
	}

	@Test
	void sellableFilterHidesExpiredAndEmptyProducts() throws Exception {
		long expired = createProduct(manager, "10.00", 5, 0, LocalDate.now().minusDays(1).toString(), true);
		long empty = createProduct(manager, "10.00", 0);
		long sellable = createProduct(manager, "10.00", 1);
		List<Integer> ids = read(get(manager, "/api/products?sellable=true"), "$[*].id");
		assertThat(ids).contains((int) sellable).doesNotContain((int) expired, (int) empty);
	}

	@Test
	void failedImageReplacementKeepsPreviousFileAndRemovesNewFile() throws Exception {
		long product = createProduct(manager, "10.00", 1);
		images.store(product, new MockMultipartFile("file", "old.png", "image/png", PNG));
		Path directory = properties.storageDir().resolve("product-images");
		String previous;
		try (var files = Files.list(directory)) {
			previous = files.filter(p -> p.getFileName().toString().startsWith(product + "-"))
				.findFirst().orElseThrow().getFileName().toString();
		}
		assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
			images.store(product, new MockMultipartFile("file", "new.png", "image/png", PNG));
			throw new IllegalStateException("abort");
		})).isInstanceOf(IllegalStateException.class);
		assertThat(Files.exists(directory.resolve(previous))).isTrue();
		try (var files = Files.list(directory)) {
			assertThat(files.filter(p -> p.getFileName().toString().startsWith(product + "-"))
				.map(p -> p.getFileName().toString()).toList()).containsExactly(previous);
		}
		get(manager, "/api/products/" + product + "/image").andExpect(status().isOk());
	}

	@Test
	void productImagesAreStoredAndServedByContentType() throws Exception {
		long product = createProduct(manager, "10.00", 1);
		mvc.perform(multipart(HttpMethod.PUT, "/api/products/" + product + "/image")
			.file(new MockMultipartFile("file", "photo.bin", "application/octet-stream", PNG))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + manager)).andExpect(status().isNoContent());
		get(manager, "/api/products/" + product + "/image").andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.IMAGE_PNG));
		get(manager, "/api/products/" + product)
			.andExpect(jsonPath("$.imageUrl").value("/api/products/" + product + "/image"));

		mvc.perform(multipart(HttpMethod.PUT, "/api/products/" + product + "/image")
			.file(new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes()))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + manager)).andExpect(status().isBadRequest());
	}

}
