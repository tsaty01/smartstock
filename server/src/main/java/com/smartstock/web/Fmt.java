package com.smartstock.web;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.smartstock.common.SmartstockProperties;

/** Display formats for templates, as {@code ${@fmt.money(amount)}}; times use the store's zone. */
@Component("fmt")
class Fmt {

	private static final Locale INDIA = Locale.forLanguageTag("en-IN");

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH);

	private final ZoneId zone;

	Fmt(SmartstockProperties properties) {
		this.zone = properties.zone();
	}

	public String money(BigDecimal amount) {
		return amount == null ? "" : NumberFormat.getCurrencyInstance(INDIA).format(amount);
	}

	public String date(LocalDate date) {
		return date == null ? "" : DATE.format(date);
	}

	public String time(Instant instant) {
		return instant == null ? "" : DATE_TIME.format(instant.atZone(zone));
	}

	public String percent(BigDecimal value) {
		return value.stripTrailingZeros().toPlainString() + "%";
	}

	public String title(Enum<?> value) {
		String name = value.name().replace('_', ' ').toLowerCase(Locale.ENGLISH);
		return value.name().length() <= 3 ? value.name() : Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

}
