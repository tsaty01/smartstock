package com.smartstock.inventory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	@Override
	@EntityGraph(attributePaths = "category")
	List<Product> findAll(Specification<Product> spec, Sort sort);

	@EntityGraph(attributePaths = "category")
	Optional<Product> findWithCategoryById(long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id = :id")
	Optional<Product> lockById(long id);

	/** Locks rows in id order so concurrent checkouts of overlapping carts cannot deadlock. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id in :ids order by p.id")
	List<Product> lockAllById(Collection<Long> ids);

	List<Product> findAllByIdIn(Collection<Long> ids);

	boolean existsByBarcode(String barcode);

	boolean existsByBarcodeAndIdNot(String barcode, long id);

}
