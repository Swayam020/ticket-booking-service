package com.swayam.booking.reservation;

import com.swayam.booking.NotFoundException;
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

	public BookingService(SeatRepository seats, BookingRepository bookings) {
		this.seats = seats;
		this.bookings = bookings;
	}

	// Check-then-act with no lock, on purpose: it refuses a seat that is already booked, but two
	// concurrent requests can both read AVAILABLE before either writes BOOKED. M4 reproduces that.
	@Transactional
	public BookingResponse book(BookingRequest request) {
		Seat seat = seats.findById(request.seatId())
				.orElseThrow(() -> new NotFoundException("seat not found"));
		if (seat.getStatus() == SeatStatus.BOOKED) {
			throw new SeatAlreadyBookedException();
		}
		seat.markBooked();
		return toResponse(bookings.save(new Booking(seat, request.customerEmail())));
	}

	@Transactional(readOnly = true)
	public BookingResponse get(UUID bookingId) {
		return bookings.findById(bookingId)
				.map(BookingService::toResponse)
				.orElseThrow(() -> new NotFoundException("booking not found"));
	}

	private static BookingResponse toResponse(Booking booking) {
		Seat seat = booking.getSeat();
		return new BookingResponse(booking.getId(), seat.getId(), seat.getShow().getId(), seat.getShow().getTitle(),
				seat.getRowLabel(), seat.getSeatNumber(), booking.getCustomerEmail(), booking.getCreatedAt());
	}

}
