package com.smartstock.reporting;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.common.SmartstockProperties;

/** Read models for dashboards and reports, as plain SQL over the transactional tables. */
@Service
@Transactional(readOnly = true)
public class ReportService {

	private static final int MONTHS = 6;

	private static final int TOP_PRODUCTS = 5;

	private static final int ALERT_LIMIT = 20;

	private final JdbcClient jdbc;

	private final Clock clock;

	private final ZoneId zone;

	private final int expiryWarningDays;

	ReportService(JdbcClient jdbc, Clock clock, SmartstockProperties properties) {
		this.jdbc = jdbc;
		this.clock = clock;
		this.zone = properties.zone();
		this.expiryWarningDays = properties.inventory().expiryWarningDays();
	}

	public Dashboard dashboard() {
		LocalDate today = LocalDate.now(clock);
		Totals todayTotals = jdbc.sql("""
				select count(*) as orders, coalesce(sum(total), 0) as revenue, coalesce(sum(tax), 0) as tax,
				       coalesce(sum(discount), 0) as discount, count(distinct customer_id) as customers
				from orders where created_at >= :from and created_at < :to""")
			.param("from", startOf(today))
			.param("to", startOf(today.plusDays(1)))
			.query(Totals.class)
			.single();
		List<MonthlySales> monthly = jdbc.sql("""
				select to_char(date_trunc('month', created_at at time zone :zone), 'YYYY-MM') as month,
				       sum(total) as revenue, count(*) as orders
				from orders where created_at >= :from
				group by 1 order by 1""")
			.param("zone", zone.getId())
			.param("from", startOf(today.withDayOfMonth(1).minusMonths(MONTHS - 1)))
			.query(MonthlySales.class)
			.list();
		List<StockAlert> lowStock = jdbc.sql("""
				select id, name, stock, reorder_level, expiry_date from products
				where active and stock <= reorder_level order by stock, name limit :limit""")
			.param("limit", ALERT_LIMIT)
			.query(StockAlert.class)
			.list();
		List<StockAlert> expiring = expiryAlerts("expiry_date between :today and :until", today);
		List<StockAlert> expired = expiryAlerts("expiry_date < :today", today);
		long unread = jdbc.sql("select count(*) from notifications where not read").query(Long.class).single();
		return new Dashboard(todayTotals, monthly, topProducts(today.minusDays(29), today, TOP_PRODUCTS), lowStock,
				expiring, expired, unread);
	}

	/** Sales per product for an inclusive date range; products without sales are listed with zeros. */
	public List<ProductSales> productSales(LocalDate from, LocalDate to) {
		return jdbc.sql("""
				select p.id, p.name, c.name as category, p.price, p.stock,
				       coalesce(sum(oi.quantity), 0) as units, coalesce(sum(oi.line_total), 0) as revenue,
				       count(distinct oi.order_id) as orders
				from products p
				join categories c on c.id = p.category_id
				left join (order_items oi join orders o on o.id = oi.order_id
				           and o.created_at >= :from and o.created_at < :to) on oi.product_id = p.id
				group by p.id, c.name
				order by revenue desc, p.name""")
			.param("from", startOf(from))
			.param("to", startOf(to.plusDays(1)))
			.query(ProductSales.class)
			.list();
	}

	public List<CategorySales> categorySales(LocalDate from, LocalDate to) {
		return jdbc.sql("""
				select c.id, c.name, coalesce(sum(oi.quantity), 0) as units, coalesce(sum(oi.line_total), 0) as revenue
				from categories c
				left join (products p join order_items oi on oi.product_id = p.id
				           join orders o on o.id = oi.order_id and o.created_at >= :from and o.created_at < :to)
				       on p.category_id = c.id
				group by c.id
				order by revenue desc, c.name""")
			.param("from", startOf(from))
			.param("to", startOf(to.plusDays(1)))
			.query(CategorySales.class)
			.list();
	}

	/** Active, in-stock products with no sale in the last {@code days} days, soonest expiry first. */
	public List<StockAlert> clearance(int days) {
		return jdbc.sql("""
				select p.id, p.name, p.stock, p.reorder_level, p.expiry_date from products p
				where p.active and p.stock > 0 and not exists (
				  select 1 from order_items oi join orders o on o.id = oi.order_id
				  where oi.product_id = p.id and o.created_at >= :since)
				order by p.expiry_date nulls last, p.name""")
			.param("since", startOf(LocalDate.now(clock).minusDays(days)))
			.query(StockAlert.class)
			.list();
	}

	private List<TopProduct> topProducts(LocalDate from, LocalDate to, int limit) {
		return jdbc.sql("""
				select p.id, p.name, sum(oi.quantity) as units, sum(oi.line_total) as revenue
				from order_items oi
				join orders o on o.id = oi.order_id
				join products p on p.id = oi.product_id
				where o.created_at >= :from and o.created_at < :to
				group by p.id order by revenue desc, p.name limit :limit""")
			.param("from", startOf(from))
			.param("to", startOf(to.plusDays(1)))
			.param("limit", limit)
			.query(TopProduct.class)
			.list();
	}

	private List<StockAlert> expiryAlerts(String condition, LocalDate today) {
		return jdbc.sql("select id, name, stock, reorder_level, expiry_date from products where active and stock > 0 and "
				+ condition + " order by expiry_date, name limit :limit")
			.param("today", today)
			.param("until", today.plusDays(expiryWarningDays))
			.param("limit", ALERT_LIMIT)
			.query(StockAlert.class)
			.list();
	}

	private OffsetDateTime startOf(LocalDate date) {
		return date.atStartOfDay(zone).toOffsetDateTime();
	}

	public record Totals(long orders, BigDecimal revenue, BigDecimal tax, BigDecimal discount, long customers) {
	}

	public record MonthlySales(String month, BigDecimal revenue, long orders) {
	}

	public record TopProduct(long id, String name, long units, BigDecimal revenue) {
	}

	public record StockAlert(long id, String name, int stock, int reorderLevel, LocalDate expiryDate) {
	}

	public record ProductSales(long id, String name, String category, BigDecimal price, int stock, long units,
			BigDecimal revenue, long orders) {
	}

	public record CategorySales(long id, String name, long units, BigDecimal revenue) {
	}

	public record Dashboard(Totals today, List<MonthlySales> monthlySales, List<TopProduct> topProducts,
			List<StockAlert> lowStock, List<StockAlert> expiringSoon, List<StockAlert> expired,
			long unreadNotifications) {
	}

}
