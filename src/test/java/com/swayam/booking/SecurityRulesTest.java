package com.swayam.booking;

import com.swayam.booking.account.AppUser;
import com.swayam.booking.account.AppUserRepository;
import com.swayam.booking.account.Role;
import com.swayam.booking.catalog.Seat;
import com.swayam.booking.catalog.SeatRepository;
import com.swayam.booking.catalog.Show;
import com.swayam.booking.catalog.ShowRepository;
import com.swayam.booking.catalog.Venue;
import com.swayam.booking.catalog.VenueRepository;
import com.swayam.booking.reservation.BookingRepository;
import com.swayam.booking.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTest {

	@Autowired MockMvc mvc;
	@Autowired AppUserRepository users;
	@Autowired TokenService tokens;
	@Autowired JwtEncoder jwtEncoder;
	@Autowired VenueRepository venues;
	@Autowired ShowRepository shows;
	@Autowired SeatRepository seats;
	@Autowired BookingRepository bookings;

	private Seat newSeat() {
		Venue venue = venues.save(new Venue("Grand Hall", "Mumbai"));
		Show show = shows.save(new Show(venue, "Hamlet", Instant.parse("2026-12-31T14:30:00Z")));
		return seats.save(new Seat(show, "A", 1));
	}

	private ResultActions createVenueAs(String authorization) throws Exception {
		var request = post("/api/venues").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "Grand Hall", "city": "Mumbai"}""");
		return mvc.perform(authorization == null ? request : request.header("Authorization", authorization));
	}

	private static String sign(JwtEncoder encoder, AppUser user, Instant issuedAt, Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder().subject(user.getId().toString()).claim("role", "USER")
				.issuedAt(issuedAt).expiresAt(expiresAt).build();
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}

	@Test
	void bookingWithoutATokenIs401() throws Exception {
		Seat seat = newSeat();

		mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("""
						{"seatId": "%s"}""".formatted(seat.getId())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value("authentication required"))
				.andExpect(jsonPath("$.timestamp").exists());
		assertThat(bookings.countBySeatId(seat.getId())).isZero();
	}

	@Test
	void readingABookingWithoutATokenIs401() throws Exception {
		mvc.perform(get("/api/bookings/{id}", UUID.randomUUID()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("authentication required"));
	}

	@Test
	void aUserCannotPromoteThemselvesByEditingTheToken() throws Exception {
		String token = tokens.issue(TestUsers.create(users, Role.USER)).accessToken();
		String[] parts = token.split("\\.");
		String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
		assertThat(payload).contains("\"role\":\"USER\"");
		String forged = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payload.replace("\"role\":\"USER\"", "\"role\":\"ADMIN\"").getBytes(StandardCharsets.UTF_8));

		// same header and signature, payload now says ADMIN: the signature no longer matches
		createVenueAs("Bearer " + parts[0] + "." + forged + "." + parts[2])
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("invalid or expired token"));
	}

	@Test
	void expiredTokenIs401() throws Exception {
		// well past the decoder's 60-second clock-skew allowance
		Instant now = Instant.now();
		String expired = sign(jwtEncoder, TestUsers.create(users, Role.USER), now.minus(Duration.ofHours(1)),
				now.minus(Duration.ofMinutes(10)));

		mvc.perform(get("/api/bookings/{id}", UUID.randomUUID()).header("Authorization", "Bearer " + expired))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("invalid or expired token"));
	}

	@Test
	void tokenSignedWithAnotherKeyIs401() throws Exception {
		byte[] otherKey = "a-different-secret-of-at-least-32-bytes".getBytes(StandardCharsets.UTF_8);
		JwtEncoder stranger = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(otherKey, "HmacSHA256"))
				.algorithm(MacAlgorithm.HS256).build();
		Instant now = Instant.now();
		String token = sign(stranger, TestUsers.create(users, Role.USER), now, now.plus(Duration.ofMinutes(5)));

		mvc.perform(get("/api/bookings/{id}", UUID.randomUUID()).header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("invalid or expired token"));
	}

	@Test
	void aUserCannotCreateVenuesOrShows() throws Exception {
		String user = TestUsers.bearer(tokens, TestUsers.create(users, Role.USER));

		createVenueAs(user)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("Forbidden"))
				.andExpect(jsonPath("$.message").value("access denied"));
		mvc.perform(post("/api/shows").header("Authorization", user).contentType(MediaType.APPLICATION_JSON).content("""
						{"venueId": "%s", "title": "Othello", "startsAt": "2026-12-31T14:30:00Z", "rows": 1, "seatsPerRow": 1}"""
						.formatted(UUID.randomUUID())))
				.andExpect(status().isForbidden());
	}

	@Test
	void anAdminCanCreateAVenue() throws Exception {
		createVenueAs(TestUsers.bearer(tokens, TestUsers.create(users, Role.ADMIN)))
				.andExpect(status().isCreated());
	}

	@Test
	void creatingAVenueWithoutATokenIs401() throws Exception {
		createVenueAs(null).andExpect(status().isUnauthorized());
	}

	@Test
	void catalogueReadsNeedNoToken() throws Exception {
		mvc.perform(get("/api/shows")).andExpect(status().isOk());
		// reaching the controller's 404 proves the request got past security
		mvc.perform(get("/api/shows/{id}/seats", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("show not found"));
	}

}
