package com.smartstock.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.smartstock.auth.User;

/** Browser sign-in principal; it only identifies the user, whose current state is reloaded per request. */
class Account extends org.springframework.security.core.userdetails.User {

	private final long id;

	Account(User user) {
		super(user.getUsername(), user.getPasswordHash(), user.isActive(), true, true, true,
				List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
		this.id = user.getId();
	}

	long id() {
		return id;
	}

}
