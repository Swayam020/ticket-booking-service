package com.swayam.booking;

import com.swayam.booking.catalog.Seat;
import com.swayam.booking.catalog.SeatRepository;
import com.swayam.booking.catalog.SeatStatus;
import com.swayam.booking.catalog.Show;
import com.swayam.booking.catalog.ShowRepository;
import com.swayam.booking.catalog.Venue;
import com.swayam.booking.catalog.VenueRepository;
import com.swayam.booking.reservation.Booking;
import com.swayam.booking.reservation.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SchemaTest {

	@Autowired VenueRepository venues;
	@Autowired ShowRepository shows;
	@Autowired SeatRepository seats;
	@Autowired BookingRepository bookings;
	@Autowired TransactionTemplate tx;

	private Show newShow(String title) {
		Venue venue = venues.save(new Venue("Grand Hall", "Mumbai"));
		return shows.save(new Show(venue, title, Instant.parse("2026-12-31T14:30:00Z")));
	}

	@Test
	void bookingLoadsItsSeatShowAndVenue() {
		Seat seat = seats.save(new Seat(newShow("Hamlet"), "A", 1));
		UUID bookingId = bookings.save(new Booking(seat, "alice@example.com")).getId();

		// a fresh transaction, so every association is read back from MySQL rather than the persistence context
		tx.executeWithoutResult(status -> {
			Booking booking = bookings.findById(bookingId).orElseThrow();
			assertThat(booking.getCustomerEmail()).isEqualTo("alice@example.com");
			assertThat(booking.getCreatedAt()).isNotNull();
			Seat loaded = booking.getSeat();
			assertThat(loaded.getRowLabel()).isEqualTo("A");
			assertThat(loaded.getSeatNumber()).isEqualTo(1);
			assertThat(loaded.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
			assertThat(loaded.getShow().getTitle()).isEqualTo("Hamlet");
			assertThat(loaded.getShow().getStartsAt()).isEqualTo(Instant.parse("2026-12-31T14:30:00Z"));
			assertThat(loaded.getShow().getVenue().getName()).isEqualTo("Grand Hall");
			assertThat(loaded.getShow().getVenue().getCity()).isEqualTo("Mumbai");
		});
	}

	@Test
	void seatsOfAShowComeBackInSeatOrder() {
		Show show = newShow("Macbeth");
		seats.save(new Seat(show, "B", 1));
		seats.save(new Seat(show, "A", 2));
		seats.save(new Seat(show, "A", 1));

		assertThat(seats.findByShowIdOrderByRowLabelAscSeatNumberAsc(show.getId()))
				.extracting(seat -> seat.getRowLabel() + seat.getSeatNumber())
				.containsExactly("A1", "A2", "B1");
	}

	@Test
	void aSeatLabelIsUniqueWithinAShow() {
		Show show = newShow("Othello");
		seats.saveAndFlush(new Seat(show, "A", 1));

		assertThatThrownBy(() -> seats.saveAndFlush(new Seat(show, "A", 1)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void theSameSeatLabelExistsInTwoShows() {
		Seat matinee = seats.saveAndFlush(new Seat(newShow("King Lear"), "A", 1));
		Seat evening = seats.saveAndFlush(new Seat(newShow("King Lear"), "A", 1));

		assertThat(seats.findById(matinee.getId())).isPresent();
		assertThat(seats.findById(evening.getId())).isPresent();
	}

	@Test
	void aSeatCanBeBookedTwice() {
		// nothing in the schema stops a double booking; M4 reproduces the race on top of this
		Seat seat = seats.save(new Seat(newShow("The Tempest"), "C", 7));
		Booking first = bookings.saveAndFlush(new Booking(seat, "alice@example.com"));
		Booking second = bookings.saveAndFlush(new Booking(seat, "bob@example.com"));

		assertThat(bookings.findAllById(List.of(first.getId(), second.getId()))).hasSize(2);
	}

}
