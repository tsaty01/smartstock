package com.smartstock.auth;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {

	private UserDtos() {
	}

	public record UserView(Long id, String username, Role role, String fullName, String phone, String email,
			boolean active, Instant createdAt) {

		public static UserView of(User user) {
			return new UserView(user.getId(), user.getUsername(), user.getRole(), user.getFullName(), user.getPhone(),
					user.getEmail(), user.isActive(), user.getCreatedAt());
		}

	}

	public record LoginRequest(@NotBlank String username, @NotBlank String password) {
	}

	public record TokenResponse(String token, Instant expiresAt, UserView user) {
	}

	/** Customer account, created by the customer or at the till. */
	public record RegisterRequest(
			@NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
			@NotBlank @Size(min = 8, max = 72) String password,
			@NotBlank @Size(max = 100) String fullName,
			@Pattern(regexp = "[0-9]{10}") String phone,
			@Email @Size(max = 254) String email) {
	}

	public record CreateUserRequest(
			@NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
			@NotBlank @Size(min = 8, max = 72) String password,
			@NotNull Role role,
			@NotBlank @Size(max = 100) String fullName,
			@Pattern(regexp = "[0-9]{10}") String phone,
			@Email @Size(max = 254) String email) {
	}

	/** @param password new password, or null to keep the current one */
	public record UpdateUserRequest(
			@NotNull Role role,
			@NotBlank @Size(max = 100) String fullName,
			@Pattern(regexp = "[0-9]{10}") String phone,
			@Email @Size(max = 254) String email,
			boolean active,
			@Size(min = 8, max = 72) String password) {
	}

}
