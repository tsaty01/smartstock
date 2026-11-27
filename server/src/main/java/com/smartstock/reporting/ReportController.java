package com.smartstock.reporting;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.common.ApiException;
import com.smartstock.reporting.ReportService.CategorySales;
import com.smartstock.reporting.ReportService.Dashboard;
import com.smartstock.reporting.ReportService.ProductSales;
import com.smartstock.reporting.ReportService.StockAlert;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
class ReportController {

	private static final int DEFAULT_RANGE_DAYS = 30;

	private final ReportService reports;

	private final Clock clock;

	ReportController(ReportService reports, Clock clock) {
		this.reports = reports;
		this.clock = clock;
	}

	@GetMapping("/dashboard")
	Dashboard dashboard() {
		return reports.dashboard();
	}

	/** Defaults to the last 30 days including today. */
	@GetMapping("/products")
	List<ProductSales> products(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		Range range = range(from, to);
		return reports.productSales(range.from(), range.to());
	}

	@GetMapping("/categories")
	List<CategorySales> categories(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		Range range = range(from, to);
		return reports.categorySales(range.from(), range.to());
	}

	@GetMapping("/clearance")
	List<StockAlert> clearance(@RequestParam(defaultValue = "30") @Positive @Max(365) int days) {
		return reports.clearance(days);
	}

	private Range range(LocalDate from, LocalDate to) {
		LocalDate end = to != null ? to : LocalDate.now(clock);
		LocalDate start = from != null ? from : end.minusDays(DEFAULT_RANGE_DAYS - 1);
		if (start.isAfter(end)) {
			throw ApiException.badRequest("from must not be after to");
		}
		return new Range(start, end);
	}

	private record Range(LocalDate from, LocalDate to) {
	}

}
