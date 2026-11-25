package com.swayam.booking.account;

import java.util.Locale;

final class Emails {

	private Emails() {
	}

	// one spelling per address, so " Alice@Example.com" can't register a second account
	static String normalise(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
