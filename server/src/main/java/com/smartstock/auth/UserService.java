package com.smartstock.auth;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.auth.UserDtos.CreateUserRequest;
import com.smartstock.auth.UserDtos.RegisterRequest;
import com.smartstock.auth.UserDtos.UpdateUserRequest;
import com.smartstock.common.ApiException;
import com.smartstock.security.CurrentUser;

@Service
public class UserService {

	private static final Sort BY_ROLE_AND_NAME = Sort.by("role", "fullName");

	private static final int CUSTOMER_SEARCH_LIMIT = 20;

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	UserService(UserRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<User> list(Role role) {
		return role == null ? users.findAll(BY_ROLE_AND_NAME) : users.findAllByRole(role, BY_ROLE_AND_NAME);
	}

	@Transactional(readOnly = true)
	public List<User> searchCustomers(String query) {
		return users.searchActiveCustomers(query.strip(), Limit.of(CUSTOMER_SEARCH_LIMIT));
	}

	@Transactional
	public User registerCustomer(RegisterRequest request) {
		return create(request.username(), request.password(), Role.CUSTOMER, request.fullName(), request.phone(),
				request.email());
	}

	@Transactional
	public User create(CreateUserRequest request) {
		return create(request.username(), request.password(), request.role(), request.fullName(), request.phone(),
				request.email());
	}

	@Transactional
	public User update(long id, UpdateUserRequest request, CurrentUser actor) {
		User user = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
		if (user.getId() == actor.id() && (!request.active() || request.role() != user.getRole())) {
			throw ApiException.rejected("You cannot disable your own account or change your own role");
		}
		user.updateProfile(request.fullName(), request.phone(), request.email());
		user.setRole(request.role());
		user.setActive(request.active());
		if (request.password() != null) {
			user.setPasswordHash(passwordEncoder.encode(request.password()));
		}
		return user;
	}

	private User create(String username, String password, Role role, String fullName, String phone, String email) {
		if (users.existsByUsernameIgnoreCase(username)) {
			throw ApiException.conflict("Username is already taken");
		}
		return users.save(new User(username, passwordEncoder.encode(password), role, fullName, phone, email));
	}

}
