package com.swayam.booking;

import com.jayway.jsonpath.JsonPath;
import com.swayam.booking.account.AppUser;
import com.swayam.booking.account.AppUserRepository;
import com.swayam.booking.catalog.Seat;
import com.swayam.booking.catalog.SeatRepository;
import com.swayam.booking.catalog.Show;
import com.swayam.booking.catalog.ShowRepository;
import com.swayam.booking.catalog.Venue;
import com.swayam.booking.catalog.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

	@Autowired MockMvc mvc;
	@Autowired AppUserRepository users;
	@Autowired VenueRepository venues;
	@Autowired ShowRepository shows;
	@Autowired SeatRepository seats;

	private static String newEmail() {
		return "alice-" + UUID.randomUUID() + "@example.com";
	}

	private ResultActions register(String email, String password) throws Exception {
		return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "%s", "password": "%s"}""".formatted(email, password)));
	}

	private ResultActions login(String email, String password) throws Exception {
		return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "%s", "password": "%s"}""".formatted(email, password)));
	}

	@Test
	void registerThenLoginGivesATokenThatBooks() throws Exception {
		String email = newEmail();
		register(email, "correct-horse")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value("USER"));

		// login normalises the email the same way register did
		String body = login(email.toUpperCase(), "correct-horse")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(body, "$.accessToken");

		Venue venue = venues.save(new Venue("Grand Hall", "Mumbai"));
		Show show = shows.save(new Show(venue, "Hamlet", Instant.parse("2026-12-31T14:30:00Z")));
		Seat seat = seats.save(new Seat(show, "A", 1));
		mvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("""
								{"seatId": "%s"}""".formatted(seat.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.customerEmail").value(email));
	}

	@Test
	void registeringTheSameEmailTwiceIs409() throws Exception {
		String email = newEmail();
		register(email, "correct-horse").andExpect(status().isCreated());

		register(email.toUpperCase(), "another-password")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("email already registered"));
	}

	@Test
	void wrongPasswordIs401() throws Exception {
		String email = newEmail();
		register(email, "correct-horse").andExpect(status().isCreated());

		login(email, "wrong-horse")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value("invalid email or password"));
	}

	@Test
	void unknownEmailIs401WithTheSameMessage() throws Exception {
		login(newEmail(), "correct-horse")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("invalid email or password"));
	}

	@Test
	void shortPasswordIs400() throws Exception {
		register(newEmail(), "short")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("password ")));
	}

	@Test
	void passwordOver72BytesIs400() throws Exception {
		// 40 characters (passes the size check) but 80 bytes in UTF-8
		register(newEmail(), "é".repeat(40))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("at most 72 bytes")));
	}

	@Test
	void passwordIsStoredHashed() throws Exception {
		String email = newEmail();
		register(email, "correct-horse").andExpect(status().isCreated());

		AppUser user = users.findByEmail(email).orElseThrow();
		assertThat(user.getPasswordHash()).isNotEqualTo("correct-horse").startsWith("$2");
	}

}
