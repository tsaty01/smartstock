package com.smartstock.inventory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findTop100ByOrderByCreatedAtDescIdDesc();

	List<Notification> findTop100ByReadFalseOrderByCreatedAtDescIdDesc();

	long countByReadFalse();

}
