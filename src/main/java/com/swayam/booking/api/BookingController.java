package com.swayam.booking.api;

import com.swayam.booking.api.Dtos.BookingRequest;
import com.swayam.booking.api.Dtos.BookingResponse;
import com.swayam.booking.reservation.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
class BookingController {

	private final BookingService bookingService;

	BookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PostMapping
	ResponseEntity<BookingResponse> book(@Valid @RequestBody BookingRequest request) {
		BookingResponse booking = bookingService.book(request);
		return ResponseEntity.created(URI.create("/api/bookings/" + booking.id())).body(booking);
	}

	@GetMapping("/{bookingId}")
	BookingResponse get(@PathVariable UUID bookingId) {
		return bookingService.get(bookingId);
	}

}
