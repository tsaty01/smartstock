package com.smartstock.auth;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.auth.UserDtos.CreateUserRequest;
import com.smartstock.auth.UserDtos.RegisterRequest;
import com.smartstock.auth.UserDtos.UpdateUserRequest;
import com.smartstock.auth.UserDtos.UserView;
import com.smartstock.security.CurrentUser;

@RestController
class UserController {

	private final UserService users;

	UserController(UserService users) {
		this.users = users;
	}

	@GetMapping("/api/users")
	@PreAuthorize("hasRole('ADMIN')")
	List<UserView> list(@RequestParam(required = false) Role role) {
		return users.list(role).stream().map(UserView::of).toList();
	}

	@PostMapping("/api/users")
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	UserView create(@Valid @RequestBody CreateUserRequest request) {
		return UserView.of(users.create(request));
	}

	@PutMapping("/api/users/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	UserView update(@PathVariable long id, @Valid @RequestBody UpdateUserRequest request,
			@AuthenticationPrincipal CurrentUser actor) {
		return UserView.of(users.update(id, request, actor));
	}

	@GetMapping("/api/customers")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
	List<UserView> searchCustomers(@RequestParam("q") String query) {
		return users.searchCustomers(query).stream().map(UserView::of).toList();
	}

	/** Registers a customer at the till without touching the staff member's own session. */
	@PostMapping("/api/customers")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
	@ResponseStatus(HttpStatus.CREATED)
	UserView registerCustomer(@Valid @RequestBody RegisterRequest request) {
		return UserView.of(users.registerCustomer(request));
	}

}
