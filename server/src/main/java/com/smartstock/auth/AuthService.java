package com.smartstock.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.auth.UserDtos.LoginRequest;
import com.smartstock.auth.UserDtos.RegisterRequest;
import com.smartstock.auth.UserDtos.TokenResponse;
import com.smartstock.auth.UserDtos.UserView;
import com.smartstock.common.ApiException;
import com.smartstock.security.TokenService;
import com.smartstock.security.TokenService.IssuedToken;

@Service
public class AuthService {

	private final UserRepository users;

	private final UserService userService;

	private final PasswordEncoder passwordEncoder;

	private final TokenService tokens;

	/** Compared against when the username is unknown, so response time does not reveal which usernames exist. */
	private final String unknownUserHash;

	AuthService(UserRepository users, UserService userService, PasswordEncoder passwordEncoder, TokenService tokens) {
		this.users = users;
		this.userService = userService;
		this.passwordEncoder = passwordEncoder;
		this.tokens = tokens;
		this.unknownUserHash = passwordEncoder.encode("unknown-user");
	}

	@Transactional(readOnly = true)
	public TokenResponse login(LoginRequest request) {
		User user = users.findByUsernameIgnoreCase(request.username()).orElse(null);
		String hash = user != null ? user.getPasswordHash() : unknownUserHash;
		if (!passwordEncoder.matches(request.password(), hash) || user == null) {
			throw ApiException.unauthorized("Invalid username or password");
		}
		if (!user.isActive()) {
			throw ApiException.forbidden("Account is disabled");
		}
		return tokenFor(user);
	}

	@Transactional
	public TokenResponse register(RegisterRequest request) {
		return tokenFor(userService.registerCustomer(request));
	}

	private TokenResponse tokenFor(User user) {
		IssuedToken token = tokens.issue(user);
		return new TokenResponse(token.value(), token.expiresAt(), UserView.of(user));
	}

}
