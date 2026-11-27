package com.smartstock.inventory;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import com.smartstock.inventory.InventoryDtos.AdjustmentRequest;
import com.smartstock.inventory.InventoryDtos.CategoryRequest;
import com.smartstock.inventory.InventoryDtos.CategoryView;
import com.smartstock.inventory.InventoryDtos.NotificationView;
import com.smartstock.inventory.InventoryDtos.ProductRequest;
import com.smartstock.inventory.InventoryDtos.ProductView;
import com.smartstock.inventory.InventoryDtos.RestockRequest;
import com.smartstock.inventory.ProductImageService.StoredImage;
import com.smartstock.security.CurrentUser;

@RestController
@RequestMapping("/api")
class InventoryController {

	private static final String MANAGEMENT = "hasAnyRole('ADMIN', 'MANAGER')";

	private final CategoryService categories;

	private final ProductService products;

	private final ProductImageService images;

	private final NotificationService notifications;

	InventoryController(CategoryService categories, ProductService products, ProductImageService images,
			NotificationService notifications) {
		this.categories = categories;
		this.products = products;
		this.images = images;
		this.notifications = notifications;
	}

	@GetMapping("/categories")
	List<CategoryView> categories() {
		return categories.list().stream().map(CategoryView::of).toList();
	}

	@PostMapping("/categories")
	@PreAuthorize(MANAGEMENT)
	@ResponseStatus(HttpStatus.CREATED)
	CategoryView createCategory(@Valid @RequestBody CategoryRequest request) {
		return CategoryView.of(categories.create(request));
	}

	@PutMapping("/categories/{id}")
	@PreAuthorize(MANAGEMENT)
	CategoryView updateCategory(@PathVariable long id, @Valid @RequestBody CategoryRequest request) {
		return CategoryView.of(categories.update(id, request));
	}

	/** Inactive products are visible to management only. */
	@GetMapping("/products")
	List<ProductView> products(@RequestParam(required = false) Long categoryId,
			@RequestParam(name = "q", required = false) String query,
			@RequestParam(defaultValue = "false") boolean includeInactive,
			@RequestParam(defaultValue = "false") boolean sellable, @AuthenticationPrincipal CurrentUser user) {
		boolean inactive = includeInactive && user.role().isManagement();
		return products.list(new ProductFilter(categoryId, query, inactive, sellable));
	}

	@GetMapping("/products/{id}")
	ProductView product(@PathVariable long id) {
		return products.get(id);
	}

	@PostMapping("/products")
	@PreAuthorize(MANAGEMENT)
	@ResponseStatus(HttpStatus.CREATED)
	ProductView createProduct(@Valid @RequestBody ProductRequest request, @AuthenticationPrincipal CurrentUser user) {
		return products.create(request, user);
	}

	@PutMapping("/products/{id}")
	@PreAuthorize(MANAGEMENT)
	ProductView updateProduct(@PathVariable long id, @Valid @RequestBody ProductRequest request) {
		return products.update(id, request);
	}

	@PostMapping("/products/{id}/restock")
	@PreAuthorize(MANAGEMENT)
	ProductView restock(@PathVariable long id, @Valid @RequestBody RestockRequest request,
			@AuthenticationPrincipal CurrentUser user) {
		return products.restock(id, request, user);
	}

	@PostMapping("/products/{id}/adjustments")
	@PreAuthorize(MANAGEMENT)
	ProductView adjust(@PathVariable long id, @Valid @RequestBody AdjustmentRequest request,
			@AuthenticationPrincipal CurrentUser user) {
		return products.adjust(id, request, user);
	}

	@PutMapping(path = "/products/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize(MANAGEMENT)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void uploadImage(@PathVariable long id, @RequestPart("file") MultipartFile file) {
		images.store(id, file);
	}

	@GetMapping("/products/{id}/image")
	ResponseEntity<Resource> image(@PathVariable long id) {
		StoredImage image = images.load(id);
		return ResponseEntity.ok().contentType(image.mediaType()).body(image.resource());
	}

	@GetMapping("/stock-movements")
	@PreAuthorize(MANAGEMENT)
	PagedModel<MovementView> movements(@RequestParam(required = false) Long productId, Pageable pageable) {
		return new PagedModel<>(products.movements(productId, pageable));
	}

	@GetMapping("/notifications")
	@PreAuthorize(MANAGEMENT)
	List<NotificationView> notifications(@RequestParam(defaultValue = "false") boolean unread) {
		return notifications.list(unread).stream().map(NotificationView::of).toList();
	}

	@PostMapping("/notifications/{id}/read")
	@PreAuthorize(MANAGEMENT)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void markRead(@PathVariable long id) {
		notifications.markRead(id);
	}

}
