package com.smartstock.inventory;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.auth.User;
import com.smartstock.auth.UserRepository;
import com.smartstock.common.ApiException;
import com.smartstock.inventory.InventoryDtos.AdjustmentRequest;
import com.smartstock.inventory.InventoryDtos.ProductRequest;
import com.smartstock.inventory.InventoryDtos.ProductView;
import com.smartstock.inventory.InventoryDtos.RestockRequest;
import com.smartstock.inventory.StockMovement.Reason;
import com.smartstock.security.CurrentUser;

/** Catalog and stock changes. Every stock change is written together with its movement in one transaction. */
@Service
public class ProductService {

	private final ProductRepository products;

	private final CategoryRepository categories;

	private final StockMovementRepository movements;

	private final UserRepository users;

	private final NotificationService notifications;

	private final Clock clock;

	ProductService(ProductRepository products, CategoryRepository categories, StockMovementRepository movements,
			UserRepository users, NotificationService notifications, Clock clock) {
		this.products = products;
		this.categories = categories;
		this.movements = movements;
		this.users = users;
		this.notifications = notifications;
		this.clock = clock;
	}

	public LocalDate today() {
		return LocalDate.now(clock);
	}

	@Transactional(readOnly = true)
	public List<ProductView> list(ProductFilter filter) {
		LocalDate today = today();
		return products.findAll(filter.toSpecification(today), Sort.by("name", "id"))
			.stream()
			.map(p -> ProductView.of(p, today))
			.toList();
	}

	@Transactional(readOnly = true)
	public ProductView get(long id) {
		return view(find(id));
	}

	@Transactional
	public ProductView create(ProductRequest request, CurrentUser actor) {
		if (request.barcode() != null && products.existsByBarcode(request.barcode())) {
			throw ApiException.conflict("Barcode is already used by another product");
		}
		int initialStock = request.initialStockOrZero();
		Product product = new Product(category(request.categoryId()), request.name(), initialStock);
		apply(product, request);
		products.save(product);
		if (initialStock > 0) {
			record(product, initialStock, Reason.INITIAL, null, actor, null);
		}
		notifications.expiryChanged(product);
		return view(product);
	}

	@Transactional
	public ProductView update(long id, ProductRequest request) {
		Product product = find(id);
		if (request.barcode() != null && products.existsByBarcodeAndIdNot(request.barcode(), id)) {
			throw ApiException.conflict("Barcode is already used by another product");
		}
		LocalDate previousExpiry = product.getExpiryDate();
		apply(product, request);
		if (!Objects.equals(previousExpiry, product.getExpiryDate())) {
			notifications.expiryChanged(product);
		}
		return view(product);
	}

	@Transactional
	public ProductView restock(long id, RestockRequest request, CurrentUser actor) {
		Product product = lock(id);
		if (!product.isActive()) {
			throw ApiException.rejected("Inactive products cannot be restocked");
		}
		product.addStock(request.quantity());
		record(product, request.quantity(), Reason.RESTOCK, null, actor, request.notes());
		return view(product);
	}

	@Transactional
	public ProductView adjust(long id, AdjustmentRequest request, CurrentUser actor) {
		if (request.reason() != Reason.ADJUSTMENT && request.reason() != Reason.EXPIRED_REMOVAL) {
			throw ApiException.badRequest("Reason must be ADJUSTMENT or EXPIRED_REMOVAL");
		}
		if (request.change() == 0 || (request.reason() == Reason.EXPIRED_REMOVAL && request.change() > 0)) {
			throw ApiException.badRequest("Change must be non-zero, and negative for expired removals");
		}
		Product product = lock(id);
		int previous = product.getStock();
		if (request.change() > 0) {
			product.addStock(request.change());
		}
		else {
			product.removeStock(-request.change());
		}
		record(product, request.change(), request.reason(), null, actor, request.notes());
		notifications.stockChanged(product, previous);
		return view(product);
	}

	/**
	 * Locks the products of a sale and returns them in id order. Callers must already be in a transaction,
	 * and complete the sale with {@link #recordSale}.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public List<Product> lockForSale(List<Long> ids) {
		List<Product> locked = products.lockAllById(ids);
		if (locked.size() != ids.size()) {
			throw ApiException.notFound("Product");
		}
		return locked;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void recordSale(Product product, int quantity, long orderId, CurrentUser actor) {
		int previous = product.getStock();
		product.removeStock(quantity);
		record(product, -quantity, Reason.SALE, orderId, actor, null);
		notifications.stockChanged(product, previous);
	}

	@Transactional(readOnly = true)
	public Page<MovementView> movements(Long productId, Pageable pageable) {
		return productId == null ? movements.findViews(pageable) : movements.findViewsByProduct(productId, pageable);
	}

	/** Must run inside the transaction that loaded the product, since the category is lazy. */
	private ProductView view(Product product) {
		return ProductView.of(product, today());
	}

	Product find(long id) {
		return products.findWithCategoryById(id).orElseThrow(() -> ApiException.notFound("Product"));
	}

	private void apply(Product product, ProductRequest r) {
		product.update(category(r.categoryId()), r.name(), r.description(), r.barcode(), r.price(), r.reorderLevel(),
				r.expiryDate(), r.warningLabel(), r.active());
	}

	private Category category(long id) {
		return categories.findById(id).orElseThrow(() -> ApiException.rejected("Category does not exist"));
	}

	private Product lock(long id) {
		return products.lockById(id).orElseThrow(() -> ApiException.notFound("Product"));
	}

	private void record(Product product, int change, Reason reason, Long orderId, CurrentUser actor, String notes) {
		User performer = users.getReferenceById(actor.id());
		movements.save(new StockMovement(product, change, reason, orderId, performer, notes));
	}

}
