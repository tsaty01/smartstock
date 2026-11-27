package com.smartstock.inventory;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.common.ApiException;
import com.smartstock.inventory.InventoryDtos.CategoryRequest;

@Service
public class CategoryService {

	private final CategoryRepository categories;

	CategoryService(CategoryRepository categories) {
		this.categories = categories;
	}

	@Transactional(readOnly = true)
	public List<Category> list() {
		return categories.findAll(Sort.by("name"));
	}

	@Transactional
	public Category create(CategoryRequest request) {
		if (categories.existsByNameIgnoreCase(request.name())) {
			throw ApiException.conflict("Category already exists");
		}
		return categories.save(new Category(request.name(), request.description()));
	}

	@Transactional
	public Category update(long id, CategoryRequest request) {
		Category category = categories.findById(id).orElseThrow(() -> ApiException.notFound("Category"));
		if (categories.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
			throw ApiException.conflict("Category already exists");
		}
		category.update(request.name(), request.description());
		return category;
	}

}
