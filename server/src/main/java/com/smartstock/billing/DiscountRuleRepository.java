package com.smartstock.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DiscountRuleRepository extends JpaRepository<DiscountRule, Long> {

	@Query("""
			select max(r.percent) from DiscountRule r
			where r.active and r.minQuantity <= :quantity and r.minAmount <= :amount
			  and (r.validFrom is null or r.validFrom <= :today)
			  and (r.validTo is null or r.validTo >= :today)""")
	Optional<BigDecimal> findBestPercent(int quantity, BigDecimal amount, LocalDate today);

}
