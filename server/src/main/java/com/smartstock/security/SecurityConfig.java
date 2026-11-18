package com.smartstock.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.smartstock.common.SmartstockProperties;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfig {

	/** Android and other API clients: stateless bearer tokens, so no CSRF exposure. */
	@Bean
	@Order(1)
	SecurityFilterChain apiSecurity(HttpSecurity http, UserJwtConverter converter) throws Exception {
		return http.securityMatcher("/api/**")
			.authorizeHttpRequests(requests -> requests
				.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.csrf(AbstractHttpConfigurer::disable)
			.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain defaultSecurity(HttpSecurity http) throws Exception {
		return http
			.authorizeHttpRequests(requests -> requests
				.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
				.anyRequest().denyAll())
			.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	JwtEncoder jwtEncoder(SmartstockProperties properties) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(tokenKey(properties)));
	}

	@Bean
	JwtDecoder jwtDecoder(SmartstockProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(tokenKey(properties))
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TokenService.ISSUER));
		return decoder;
	}

	private static SecretKey tokenKey(SmartstockProperties properties) {
		return new SecretKeySpec(properties.security().tokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

}
