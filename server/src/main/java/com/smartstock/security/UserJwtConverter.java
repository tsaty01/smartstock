package com.smartstock.security;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.smartstock.auth.User;
import com.smartstock.auth.UserRepository;

/**
 * Resolves the token subject to the current user record, so a disabled account or a changed role
 * takes effect immediately instead of when the token expires.
 */
@Component
class UserJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private final UserRepository users;

	UserJwtConverter(UserRepository users) {
		this.users = users;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		User user = users.findById(Long.valueOf(jwt.getSubject()))
			.filter(User::isActive)
			.orElseThrow(() -> new DisabledException("Account is disabled"));
		return UsernamePasswordAuthenticationToken.authenticated(CurrentUser.of(user), jwt,
				List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
	}

}
