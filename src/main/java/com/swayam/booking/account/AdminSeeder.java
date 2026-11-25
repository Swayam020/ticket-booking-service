package com.swayam.booking.account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

// The only way to get an ADMIN: register always creates USER, so nobody can sign themselves up as one.
// An existing account with this email is left as it is (never promoted, password never reset).
@Component
public class AdminSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

	private final AppUserRepository users;
	private final PasswordEncoder encoder;
	private final String email;
	private final String password;

	public AdminSeeder(AppUserRepository users, PasswordEncoder encoder,
			@Value("${app.admin.email}") String email, @Value("${app.admin.password}") String password) {
		this.users = users;
		this.encoder = encoder;
		this.email = email;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (email.isBlank() || password.isBlank()) {
			return;
		}
		// same rule as RegisterRequest: BCrypt refuses more than 72 bytes
		if (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new IllegalStateException("ADMIN_PASSWORD must be at least 8 characters and at most 72 bytes");
		}
		String normalised = Emails.normalise(email);
		if (users.existsByEmail(normalised)) {
			return;
		}
		users.save(new AppUser(normalised, encoder.encode(password), Role.ADMIN));
		log.info("created admin account {}", normalised);
	}

}
