package com.swayam.booking.api;

import com.swayam.booking.catalog.SeatStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class Dtos {

	private Dtos() {
	}

	public record CreateVenueRequest(
			@NotBlank @Size(max = 100) String name,
			@NotBlank @Size(max = 100) String city) {
	}

	public record VenueResponse(UUID id, String name, String city) {
	}

	// rows are labelled A..Z, hence the cap of 26
	public record CreateShowRequest(
			@NotNull UUID venueId,
			@NotBlank @Size(max = 200) String title,
			@NotNull Instant startsAt,
			@Min(1) @Max(26) int rows,
			@Min(1) @Max(50) int seatsPerRow) {
	}

	public record ShowResponse(UUID id, String title, Instant startsAt, VenueResponse venue) {
	}

	public record SeatResponse(UUID id, String row, int number, SeatStatus status) {
	}

	public record BookingRequest(
			@NotNull UUID seatId,
			@NotBlank @Email @Size(max = 255) String customerEmail) {
	}

	public record BookingResponse(UUID id, UUID seatId, UUID showId, String showTitle, String row, int number,
			String customerEmail, Instant createdAt) {
	}

	public record ApiError(String error, String message, Instant timestamp) {
	}

}
