package com.smartstock.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.auth.UserDtos.LoginRequest;
import com.smartstock.auth.UserDtos.RegisterRequest;
import com.smartstock.auth.UserDtos.TokenResponse;
import com.smartstock.auth.UserDtos.UserView;
import com.smartstock.common.ApiException;
import com.smartstock.security.CurrentUser;

@RestController
@RequestMapping("/api/auth")
class AuthController {

	private final AuthService auth;

	private final UserRepository users;

	AuthController(AuthService auth, UserRepository users) {
		this.auth = auth;
		this.users = users;
	}

	@PostMapping("/login")
	TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return auth.login(request);
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	TokenResponse register(@Valid @RequestBody RegisterRequest request) {
		return auth.register(request);
	}

	@GetMapping("/me")
	UserView me(@AuthenticationPrincipal CurrentUser user) {
		return users.findById(user.id()).map(UserView::of).orElseThrow(() -> ApiException.notFound("User"));
	}

}
