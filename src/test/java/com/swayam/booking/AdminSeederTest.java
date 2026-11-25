package com.swayam.booking;

import com.swayam.booking.account.AdminSeeder;
import com.swayam.booking.account.AppUser;
import com.swayam.booking.account.AppUserRepository;
import com.swayam.booking.account.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The seeders here are built by hand, so each test picks its own email/password without a second Spring context
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AdminSeederTest {

	@Autowired AppUserRepository users;
	@Autowired PasswordEncoder encoder;

	private AdminSeeder seeder(String email, String password) {
		return new AdminSeeder(users, encoder, email, password);
	}

	@Test
	void createsTheAdminOnceWithAHashedPassword() {
		String email = "admin-" + UUID.randomUUID() + "@example.com";
		seeder(email.toUpperCase(), "admin-password-1").run(null);

		AppUser admin = users.findByEmail(email).orElseThrow();
		assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
		assertThat(encoder.matches("admin-password-1", admin.getPasswordHash())).isTrue();

		// a restart with a different password changes nothing
		seeder(email, "admin-password-2").run(null);
		assertThat(users.findByEmail(email).orElseThrow().getPasswordHash()).isEqualTo(admin.getPasswordHash());
	}

	@Test
	void blankPasswordCreatesNobody() {
		String email = "admin-" + UUID.randomUUID() + "@example.com";
		seeder(email, "").run(null);

		assertThat(users.existsByEmail(email)).isFalse();
	}

	@Test
	void shortPasswordFailsStartup() {
		assertThatThrownBy(() -> seeder("admin-" + UUID.randomUUID() + "@example.com", "short").run(null))
				.isInstanceOf(IllegalStateException.class);
	}

}
