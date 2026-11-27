package com.smartstock.common;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("smartstock")
public record SmartstockProperties(
		@NotNull ZoneId zone,
		@NotNull Path storageDir,
		@Valid @NotNull Billing billing,
		@Valid @NotNull Inventory inventory,
		@Valid @NotNull Security security) {

	/** @param taxRate percentage applied to the discounted subtotal */
	public record Billing(@NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal taxRate) {
	}

	public record Inventory(@PositiveOrZero int expiryWarningDays) {
	}

	/** @param tokenSecret HMAC key for API tokens; at least 32 bytes, never committed */
	public record Security(@NotNull @Size(min = 32) String tokenSecret, @NotNull Duration tokenTtl) {
	}

}
