package com.smartstock.billing;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

	String SUMMARY = """
			select new com.smartstock.billing.OrderSummary(
			  o.id, o.createdAt, c.id, c.fullName, k.fullName, o.paymentMode, size(o.items), o.total)
			from Order o join o.customer c join o.cashier k""";

	@Query(value = SUMMARY + " order by o.createdAt desc, o.id desc", countQuery = "select count(o) from Order o")
	Page<OrderSummary> findSummaries(Pageable pageable);

	@Query(value = SUMMARY + " where c.id = :customerId order by o.createdAt desc, o.id desc",
			countQuery = "select count(o) from Order o where o.customer.id = :customerId")
	Page<OrderSummary> findSummariesByCustomer(long customerId, Pageable pageable);

	@EntityGraph(attributePaths = { "customer", "cashier", "items" })
	Optional<Order> findWithDetailsById(long id);

}
