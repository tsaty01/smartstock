package com.smartstock.web;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartstock.inventory.CategoryService;
import com.smartstock.inventory.InventoryDtos.AdjustmentRequest;
import com.smartstock.inventory.InventoryDtos.CategoryRequest;
import com.smartstock.inventory.InventoryDtos.ProductRequest;
import com.smartstock.inventory.InventoryDtos.ProductView;
import com.smartstock.inventory.InventoryDtos.RestockRequest;
import com.smartstock.inventory.ProductFilter;
import com.smartstock.inventory.ProductImageService;
import com.smartstock.inventory.ProductImageService.StoredImage;
import com.smartstock.inventory.ProductService;
import com.smartstock.security.CurrentUser;

@Controller
class ProductPages {

	private static final String MANAGEMENT = "hasAnyRole('ADMIN', 'MANAGER')";

	private static final int HISTORY_SIZE = 50;

	static final List<String> WARNING_LABELS = List.of("Prescription Required", "Harmful to Kids", "Age 18+ Only",
			"Allergen: Contains Nuts", "Refrigerate After Opening", "Restricted Drug");

	/** Stock filters offered on the product list. */
	private static final Map<String, Predicate<ProductView>> FILTERS = Map.of(
			"low", p -> p.lowStock() && p.stock() > 0,
			"out", p -> p.stock() == 0,
			"expiring", p -> p.daysToExpiry() != null && p.daysToExpiry() >= 0 && p.daysToExpiry() <= 30,
			"expired", ProductView::expired,
			"inactive", p -> !p.active());

	private final ProductService products;

	private final CategoryService categories;

	private final ProductImageService images;

	ProductPages(ProductService products, CategoryService categories, ProductImageService images) {
		this.products = products;
		this.categories = categories;
		this.images = images;
	}

	@GetMapping("/products")
	@PreAuthorize(MANAGEMENT)
	String list(@RequestParam(name = "q", required = false) String query,
			@RequestParam(required = false) Long categoryId, @RequestParam(defaultValue = "all") String show,
			Model model) {
		List<ProductView> list = products.list(new ProductFilter(categoryId, query, true, false));
		if (FILTERS.containsKey(show)) {
			list = list.stream().filter(FILTERS.get(show)).toList();
		}
		model.addAttribute("products", list);
		model.addAttribute("categories", categories.list());
		model.addAttribute("q", query);
		model.addAttribute("categoryId", categoryId);
		model.addAttribute("show", show);
		return "products/list";
	}

	/** Catalog for customers: products that can be bought today. */
	@GetMapping("/shop")
	String shop(@RequestParam(name = "q", required = false) String query,
			@RequestParam(required = false) Long categoryId, Model model) {
		model.addAttribute("products", products.list(new ProductFilter(categoryId, query, false, true)));
		model.addAttribute("categories", categories.list());
		model.addAttribute("q", query);
		model.addAttribute("categoryId", categoryId);
		return "products/shop";
	}

	@GetMapping("/products/{id}")
	String detail(@PathVariable long id, Model model, @AuthenticationPrincipal CurrentUser me) {
		model.addAttribute("p", products.get(id));
		if (me.role().isManagement()) {
			model.addAttribute("movements", products.movements(id, PageRequest.of(0, HISTORY_SIZE)).getContent());
		}
		return "products/detail";
	}

	@GetMapping("/products/{id}/image")
	ResponseEntity<Resource> image(@PathVariable long id) {
		StoredImage image = images.load(id);
		return ResponseEntity.ok().contentType(image.mediaType()).body(image.resource());
	}

	@GetMapping("/products/new")
	@PreAuthorize(MANAGEMENT)
	String newForm(Model model) {
		Forms.empty(model, null);
		return form(model, null);
	}

	@PostMapping("/products/new")
	@PreAuthorize(MANAGEMENT)
	String create(@Valid @ModelAttribute("form") ProductRequest form, BindingResult result, Model model,
			@AuthenticationPrincipal CurrentUser me, RedirectAttributes redirect) {
		if (!Forms.check(model, form, result)) {
			return form(model, null);
		}
		ProductView created = products.create(form, me);
		redirect.addFlashAttribute(WebSupport.NOTICE, "Product added");
		return "redirect:/products/" + created.id();
	}

	@GetMapping("/products/{id}/edit")
	@PreAuthorize(MANAGEMENT)
	String editForm(@PathVariable long id, Model model) {
		ProductView p = products.get(id);
		Forms.empty(model, new ProductRequest(p.categoryId(), p.name(), p.description(), p.barcode(), p.price(),
				p.reorderLevel(), p.expiryDate(), p.warningLabel(), p.active(), null));
		return form(model, id);
	}

	@PostMapping("/products/{id}/edit")
	@PreAuthorize(MANAGEMENT)
	String update(@PathVariable long id, @Valid @ModelAttribute("form") ProductRequest form, BindingResult result,
			Model model, RedirectAttributes redirect) {
		if (!Forms.check(model, form, result)) {
			return form(model, id);
		}
		products.update(id, form);
		redirect.addFlashAttribute(WebSupport.NOTICE, "Product saved");
		return "redirect:/products/" + id;
	}

	@PostMapping("/products/{id}/restock")
	@PreAuthorize(MANAGEMENT)
	String restock(@PathVariable long id, @Valid RestockRequest request, BindingResult result,
			@AuthenticationPrincipal CurrentUser me, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			redirect.addFlashAttribute(WebSupport.ERROR, "Enter a quantity of at least 1");
		}
		else {
			products.restock(id, request, me);
			redirect.addFlashAttribute(WebSupport.NOTICE, "Added " + request.quantity() + " to stock");
		}
		return "redirect:/products/" + id;
	}

	@PostMapping("/products/{id}/adjust")
	@PreAuthorize(MANAGEMENT)
	String adjust(@PathVariable long id, @Valid AdjustmentRequest request, BindingResult result,
			@AuthenticationPrincipal CurrentUser me, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			redirect.addFlashAttribute(WebSupport.ERROR, "Enter a change and a reason");
		}
		else {
			products.adjust(id, request, me);
			redirect.addFlashAttribute(WebSupport.NOTICE, "Stock adjusted");
		}
		return "redirect:/products/" + id;
	}

	@PostMapping("/products/{id}/image")
	@PreAuthorize(MANAGEMENT)
	String uploadImage(@PathVariable long id, @RequestParam("file") MultipartFile file, RedirectAttributes redirect) {
		images.store(id, file);
		redirect.addFlashAttribute(WebSupport.NOTICE, "Image updated");
		return "redirect:/products/" + id;
	}

	@GetMapping("/categories")
	@PreAuthorize(MANAGEMENT)
	String categories(Model model) {
		model.addAttribute("categories", categories.list());
		return "products/categories";
	}

	@PostMapping("/categories")
	@PreAuthorize(MANAGEMENT)
	String createCategory(@Valid CategoryRequest request, BindingResult result, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			redirect.addFlashAttribute(WebSupport.ERROR, "Enter a category name");
			return "redirect:/categories";
		}
		categories.create(request);
		redirect.addFlashAttribute(WebSupport.NOTICE, "Category added");
		return "redirect:/categories";
	}

	@PostMapping("/categories/{id}")
	@PreAuthorize(MANAGEMENT)
	String updateCategory(@PathVariable long id, @Valid CategoryRequest request, BindingResult result,
			RedirectAttributes redirect) {
		if (result.hasErrors()) {
			redirect.addFlashAttribute(WebSupport.ERROR, "Enter a category name");
			return "redirect:/categories";
		}
		categories.update(id, request);
		redirect.addFlashAttribute(WebSupport.NOTICE, "Category saved");
		return "redirect:/categories";
	}

	private String form(Model model, Long id) {
		model.addAttribute("id", id);
		model.addAttribute("categories", categories.list());
		model.addAttribute("warningLabels", WARNING_LABELS);
		return "products/form";
	}

}
