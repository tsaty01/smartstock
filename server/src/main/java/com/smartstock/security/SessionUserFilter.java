package com.smartstock.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.smartstock.auth.User;
import com.smartstock.auth.UserRepository;

/**
 * Reloads the signed-in browser user on every request, like the API does for tokens: a disabled account is
 * signed out at once and a role change applies to the next page.
 */
class SessionUserFilter extends OncePerRequestFilter {

	private final UserRepository users;

	SessionUserFilter(UserRepository users) {
		this.users = users;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Account account) {
			Optional<User> user = users.findById(account.id()).filter(User::isActive);
			if (user.isEmpty()) {
				HttpSession session = request.getSession(false);
				if (session != null) {
					session.invalidate();
				}
				SecurityContextHolder.clearContext();
				response.sendRedirect(request.getContextPath() + "/login?disabled");
				return;
			}
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(CurrentUser.of(user.get()), null,
					List.of(new SimpleGrantedAuthority("ROLE_" + user.get().getRole().name()))));
			SecurityContextHolder.setContext(context);
		}
		chain.doFilter(request, response);
	}

}
