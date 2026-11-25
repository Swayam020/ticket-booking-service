package com.swayam.booking.reservation;

import com.swayam.booking.NotFoundException;
import com.swayam.booking.account.AppUser;
import com.swayam.booking.account.AppUserRepository;
import com.swayam.booking.api.Dtos.BookingRequest;
import com.swayam.booking.api.Dtos.BookingResponse;
import com.swayam.booking.catalog.Seat;
import com.swayam.booking.catalog.SeatRepository;
import com.swayam.booking.catalog.SeatStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BookingService {

	private final SeatRepository seats;
	private final BookingRepository bookings;
	private final AppUserRepository users;

	public BookingService(SeatRepository seats, BookingRepository bookings, AppUserRepository users) {
		this.seats = seats;
		this.bookings = bookings;
		this.users = users;
	}

	// Check-then-act with no lock, on purpose: it refuses a seat that is already booked, but two
	// concurrent requests can both read AVAILABLE before either writes BOOKED. M4 reproduces that.
	@Transactional
	public BookingResponse book(UUID userId, BookingRequest request) {
		// only a validly signed token for an account that no longer exists gets here
		AppUser user = users.findById(userId).orElseThrow(() -> new NotFoundException("user not found"));
		Seat seat = seats.findById(request.seatId())
				.orElseThrow(() -> new NotFoundException("seat not found"));
		if (seat.getStatus() == SeatStatus.BOOKED) {
			throw new SeatAlreadyBookedException();
		}
		seat.markBooked();
		return toResponse(bookings.save(new Booking(seat, user)));
	}

	@Transactional(readOnly = true)
	// someone else's booking is "not found" rather than 403, so booking ids can't be probed for existence
	public BookingResponse get(UUID bookingId, UUID userId) {
		return bookings.findByIdAndUserId(bookingId, userId)
				.map(BookingService::toResponse)
				.orElseThrow(() -> new NotFoundException("booking not found"));
	}

	private static BookingResponse toResponse(Booking booking) {
		Seat seat = booking.getSeat();
		return new BookingResponse(booking.getId(), seat.getId(), seat.getShow().getId(), seat.getShow().getTitle(),
				seat.getRowLabel(), seat.getSeatNumber(), booking.getUser().getEmail(), booking.getCreatedAt());
	}

}
