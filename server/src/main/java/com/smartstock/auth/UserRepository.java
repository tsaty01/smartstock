package com.smartstock.auth;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsernameIgnoreCase(String username);

	boolean existsByUsernameIgnoreCase(String username);

	List<User> findAllByRole(Role role, Sort sort);

	@Query("""
			select u from User u
			where u.role = com.smartstock.auth.Role.CUSTOMER and u.active
			  and (lower(u.fullName) like lower(concat('%', :query, '%'))
			    or lower(u.username) like lower(concat('%', :query, '%'))
			    or u.phone like concat('%', :query, '%'))
			order by u.fullName""")
	List<User> searchActiveCustomers(String query, Limit limit);

}
