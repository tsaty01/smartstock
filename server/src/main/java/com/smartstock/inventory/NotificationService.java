package com.smartstock.inventory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.common.ApiException;
import com.smartstock.common.SmartstockProperties;
import com.smartstock.inventory.Notification.Type;

/** Raises inventory alerts as part of the stock or product change that causes them. */
@Service
public class NotificationService {

	private final NotificationRepository notifications;

	private final Clock clock;

	private final int expiryWarningDays;

	NotificationService(NotificationRepository notifications, Clock clock, SmartstockProperties properties) {
		this.notifications = notifications;
		this.clock = clock;
		this.expiryWarningDays = properties.inventory().expiryWarningDays();
	}

	/** Alerts once, when stock first falls to or below the reorder level. */
	void stockChanged(Product product, int previousStock) {
		if (previousStock > product.getReorderLevel() && product.isLowStock()) {
			notifications.save(new Notification(Type.LOW_STOCK,
					"Low stock: \"%s\" has %d left (reorder level %d)".formatted(product.getName(),
							product.getStock(), product.getReorderLevel()),
					product.getId()));
		}
	}

	/** Alerts when a product is saved with an expiry date inside the warning window. */
	void expiryChanged(Product product) {
		LocalDate expiry = product.getExpiryDate();
		if (expiry == null) {
			return;
		}
		long days = ChronoUnit.DAYS.between(LocalDate.now(clock), expiry);
		if (days >= 0 && days <= expiryWarningDays) {
			notifications.save(new Notification(Type.EXPIRY,
					"Expiring: \"%s\" expires on %s (%d days)".formatted(product.getName(), expiry, days),
					product.getId()));
		}
	}

	@Transactional(readOnly = true)
	public List<Notification> list(boolean unreadOnly) {
		return unreadOnly ? notifications.findTop100ByReadFalseOrderByCreatedAtDescIdDesc()
				: notifications.findTop100ByOrderByCreatedAtDescIdDesc();
	}

	@Transactional
	public void markRead(long id) {
		notifications.findById(id).orElseThrow(() -> ApiException.notFound("Notification")).markRead();
	}

	@Transactional(readOnly = true)
	public long unreadCount() {
		return notifications.countByReadFalse();
	}

}
