package com.smartstock.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

	String VIEW = """
			select new com.smartstock.inventory.MovementView(
			  m.id, p.id, p.name, m.change, m.reason, m.orderId, u.fullName, m.notes, m.createdAt)
			from StockMovement m join m.product p join m.performedBy u""";

	@Query(value = VIEW + " order by m.createdAt desc, m.id desc", countQuery = "select count(m) from StockMovement m")
	Page<MovementView> findViews(Pageable pageable);

	@Query(value = VIEW + " where p.id = :productId order by m.createdAt desc, m.id desc",
			countQuery = "select count(m) from StockMovement m where m.product.id = :productId")
	Page<MovementView> findViewsByProduct(long productId, Pageable pageable);

	long countByProductId(long productId);

}
