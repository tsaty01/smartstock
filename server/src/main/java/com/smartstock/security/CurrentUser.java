package com.smartstock.security;

import com.smartstock.auth.Role;
import com.smartstock.auth.User;

/** The authenticated principal, rebuilt from the database on every request. */
public record CurrentUser(long id, String username, Role role) {

	public static CurrentUser of(User user) {
		return new CurrentUser(user.getId(), user.getUsername(), user.getRole());
	}

}
