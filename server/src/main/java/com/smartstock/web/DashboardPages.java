package com.smartstock.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.smartstock.common.ApiException;
import com.smartstock.inventory.NotificationService;
import com.smartstock.reporting.ReportService;
import com.smartstock.reporting.ReportService.Dashboard;
import com.smartstock.reporting.ReportService.MonthlySales;

/** Dashboard, reports and alerts for management. */
@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
class DashboardPages {

	private static final int MONTHS = 6;

	private static final int DEFAULT_RANGE_DAYS = 30;

	private final ReportService reports;

	private final NotificationService notifications;

	private final Clock clock;

	DashboardPages(ReportService reports, NotificationService notifications, Clock clock) {
		this.reports = reports;
		this.notifications = notifications;
		this.clock = clock;
	}

	@GetMapping("/dashboard")
	String dashboard(Model model) {
		Dashboard dashboard = reports.dashboard();
		model.addAttribute("d", dashboard);
		model.addAttribute("bars", bars(dashboard.monthlySales()));
		return "dashboard";
	}

	@GetMapping("/reports")
	String reports(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, Model model) {
		LocalDate end = to != null ? to : LocalDate.now(clock);
		LocalDate start = from != null ? from : end.minusDays(DEFAULT_RANGE_DAYS - 1);
		if (start.isAfter(end)) {
			throw ApiException.badRequest("The start date must not be after the end date");
		}
		model.addAttribute("from", start);
		model.addAttribute("to", end);
		model.addAttribute("categories", reports.categorySales(start, end));
		model.addAttribute("products", reports.productSales(start, end));
		model.addAttribute("clearance", reports.clearance(DEFAULT_RANGE_DAYS));
		return "reports";
	}

	@GetMapping("/alerts")
	String alerts(Model model) {
		model.addAttribute("alerts", notifications.list(false));
		return "alerts";
	}

	@PostMapping("/alerts/{id}/read")
	String markRead(@PathVariable long id) {
		notifications.markRead(id);
		return "redirect:/alerts";
	}

	/** Six months ending this month, with months without sales shown as empty bars. */
	private List<Bar> bars(List<MonthlySales> sales) {
		Map<String, BigDecimal> byMonth = sales.stream()
			.collect(Collectors.toMap(MonthlySales::month, MonthlySales::revenue));
		YearMonth now = YearMonth.now(clock);
		List<YearMonth> months = IntStream.range(0, MONTHS).mapToObj(i -> now.minusMonths(MONTHS - 1 - i)).toList();
		BigDecimal max = months.stream()
			.map(m -> byMonth.getOrDefault(m.toString(), BigDecimal.ZERO))
			.max(BigDecimal::compareTo)
			.filter(m -> m.signum() > 0)
			.orElse(BigDecimal.ONE);
		return months.stream().map(m -> {
			BigDecimal revenue = byMonth.getOrDefault(m.toString(), BigDecimal.ZERO);
			int percent = revenue.multiply(BigDecimal.valueOf(100)).divide(max, 0, RoundingMode.HALF_UP).intValue();
			return new Bar(m.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), revenue, percent);
		}).toList();
	}

	record Bar(String label, BigDecimal revenue, int percent) {
	}

}
