package com.swayam.booking;

import com.jayway.jsonpath.JsonPath;
import com.swayam.booking.catalog.Seat;
import com.swayam.booking.catalog.SeatRepository;
import com.swayam.booking.catalog.Show;
import com.swayam.booking.catalog.ShowRepository;
import com.swayam.booking.catalog.Venue;
import com.swayam.booking.catalog.VenueRepository;
import com.swayam.booking.reservation.BookingRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BookingApiTest {

	@Autowired MockMvc mvc;
	@Autowired VenueRepository venues;
	@Autowired ShowRepository shows;
	@Autowired SeatRepository seats;
	@Autowired BookingRepository bookings;

	private Seat newSeat(String title) {
		Venue venue = venues.save(new Venue("Grand Hall", "Mumbai"));
		Show show = shows.save(new Show(venue, title, Instant.parse("2026-12-31T14:30:00Z")));
		return seats.save(new Seat(show, "C", 7));
	}

	private ResultActions book(Object seatId, String email) throws Exception {
		return mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("""
				{"seatId": "%s", "customerEmail": "%s"}""".formatted(seatId, email)));
	}

	@Test
	void bookingAFreeSeatSucceedsAndMarksItBooked() throws Exception {
		Seat seat = newSeat("Hamlet");
		UUID showId = seat.getShow().getId();

		String body = book(seat.getId(), "alice@example.com")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", containsString("/api/bookings/")))
				.andExpect(jsonPath("$.seatId").value(seat.getId().toString()))
				.andExpect(jsonPath("$.showId").value(showId.toString()))
				.andExpect(jsonPath("$.showTitle").value("Hamlet"))
				.andExpect(jsonPath("$.row").value("C"))
				.andExpect(jsonPath("$.number").value(7))
				.andExpect(jsonPath("$.customerEmail").value("alice@example.com"))
				.andExpect(jsonPath("$.createdAt").exists())
				.andReturn().getResponse().getContentAsString();
		String bookingId = JsonPath.read(body, "$.id");

		mvc.perform(get("/api/shows/{id}/seats", showId))
				.andExpect(jsonPath("$[0].status").value("BOOKED"));
		mvc.perform(get("/api/bookings/{id}", bookingId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(bookingId))
				.andExpect(jsonPath("$.seatId").value(seat.getId().toString()))
				.andExpect(jsonPath("$.customerEmail").value("alice@example.com"));
	}

	@Test
	void bookingATakenSeatIs409AndKeepsOneBooking() throws Exception {
		Seat seat = newSeat("Macbeth");
		book(seat.getId(), "alice@example.com").andExpect(status().isCreated());

		book(seat.getId(), "bob@example.com")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Conflict"))
				.andExpect(jsonPath("$.message").value("seat already booked"));

		assertThat(bookings.countBySeatId(seat.getId())).isEqualTo(1);
	}

	@Test
	void bookingAnUnknownSeatIs404() throws Exception {
		book(UUID.randomUUID(), "alice@example.com")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("seat not found"));
	}

	@Test
	void bookingNeedsAValidEmail() throws Exception {
		Seat seat = newSeat("Othello");

		book(seat.getId(), "not-an-email")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("customerEmail ")));
		assertThat(bookings.countBySeatId(seat.getId())).isZero();
	}

	@Test
	void anUnknownBookingIs404() throws Exception {
		mvc.perform(get("/api/bookings/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("booking not found"));
	}

}
