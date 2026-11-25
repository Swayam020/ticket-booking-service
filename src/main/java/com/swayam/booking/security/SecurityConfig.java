package com.swayam.booking.security;

import com.swayam.booking.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Configuration
public class SecurityConfig {

	static final String ROLE_CLAIM = "role";

	private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

	// 401 and 403 are raised in the filter chain, before any controller runs, so @RestControllerAdvice never sees
	// them on its own; handing them to the MVC exception resolver gives them the same ApiError body as every other error
	@Bean
	SecurityFilterChain api(HttpSecurity http,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
		AuthenticationEntryPoint unauthorized = (request, response, e) -> resolver.resolveException(request, response,
				null, new UnauthorizedException(e instanceof OAuth2AuthenticationException
						? "invalid or expired token" : "authentication required"));
		AccessDeniedHandler forbidden = (request, response, e) -> resolver.resolveException(request, response, null, e);

		http.csrf(AbstractHttpConfigurer::disable) // no cookies or sessions, so nothing for CSRF to ride on
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/shows", "/api/shows/*/seats").permitAll()
						.requestMatchers("/actuator/health", "/error").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/venues", "/api/shows").hasRole("ADMIN")
						.anyRequest().authenticated())
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(jwt -> jwt.jwtAuthenticationConverter(roleClaimAsAuthority()))
						.authenticationEntryPoint(unauthorized)
						.accessDeniedHandler(forbidden))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(unauthorized)
						.accessDeniedHandler(forbidden));
		return http.build();
	}

	// "role": "ADMIN" -> ROLE_ADMIN, which is what hasRole("ADMIN") checks for
	private static JwtAuthenticationConverter roleClaimAsAuthority() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName(ROLE_CLAIM);
		authorities.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	@Bean
	SecretKey jwtKey(@Value("${app.jwt.secret}") String secret) {
		if (secret.isBlank()) {
			log.warn("JWT_SECRET is not set: signing with a random key, so every token dies when the app restarts");
			byte[] random = new byte[32];
			new SecureRandom().nextBytes(random);
			return new SecretKeySpec(random, "HmacSHA256");
		}
		byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtKey) {
		return NimbusJwtEncoder.withSecretKey(jwtKey).algorithm(MacAlgorithm.HS256).build();
	}

	// pinned to HS256: a token claiming any other algorithm (including "none") is rejected
	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtKey) {
		return NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
