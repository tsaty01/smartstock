package com.smartstock.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String username;

	private String passwordHash;

	@Enumerated(EnumType.STRING)
	private Role role;

	private String fullName;

	private String phone;

	private String email;

	private boolean active = true;

	@Column(updatable = false)
	private Instant createdAt = Instant.now();

	protected User() {
	}

	public User(String username, String passwordHash, Role role, String fullName, String phone, String email) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.role = role;
		updateProfile(fullName, phone, email);
	}

	public void updateProfile(String fullName, String phone, String email) {
		this.fullName = fullName;
		this.phone = phone;
		this.email = email;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public String getFullName() {
		return fullName;
	}

	public String getPhone() {
		return phone;
	}

	public String getEmail() {
		return email;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
