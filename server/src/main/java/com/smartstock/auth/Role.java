package com.smartstock.auth;

public enum Role {

	ADMIN, MANAGER, CASHIER, CUSTOMER;

	public boolean isStaff() {
		return this != CUSTOMER;
	}

	public boolean isManagement() {
		return this == ADMIN || this == MANAGER;
	}

}
