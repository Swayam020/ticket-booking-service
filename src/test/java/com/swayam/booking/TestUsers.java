package com.swayam.booking;

import com.swayam.booking.account.AppUser;
import com.swayam.booking.account.AppUserRepository;
import com.swayam.booking.account.Role;
import com.swayam.booking.security.TokenService;

import java.util.UUID;

// Users made straight in the database, with tokens minted directly: tests that aren't about login skip it.
// Each email is unique because every test class shares one database.
final class TestUsers {

	private TestUsers() {
	}

	static AppUser create(AppUserRepository users, Role role) {
		return users.save(new AppUser("user-" + UUID.randomUUID() + "@example.com", "not-a-real-hash", role));
	}

	static String bearer(TokenService tokens, AppUser user) {
		return "Bearer " + tokens.issue(user).accessToken();
	}

}
