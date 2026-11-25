package com.swayam.booking.api;

import com.swayam.booking.account.Role;
import com.swayam.booking.catalog.SeatStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;
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

	// the customer is whoever the bearer token belongs to, so it is not part of the body
	public record BookingRequest(@NotNull UUID seatId) {
	}

	public record BookingResponse(UUID id, UUID seatId, UUID showId, String showTitle, String row, int number,
			String customerEmail, Instant createdAt) {
	}

	// BCrypt refuses more than 72 bytes, and one character can take up to 4 bytes in UTF-8, so the
	// character limit alone would let a 72-character password through to a 500
	public record RegisterRequest(
			@NotBlank @Email @Size(max = 255) String email,
			@NotBlank @Size(min = 8, max = 72) String password) {

		@AssertTrue(message = "must be at most 72 bytes")
		public boolean isPasswordWithin72Bytes() {
			return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
		}
	}

	public record LoginRequest(@NotBlank String email, @NotBlank String password) {
	}

	public record UserResponse(UUID id, String email, Role role) {
	}

	public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
	}

	public record ApiError(String error, String message, Instant timestamp) {
	}

}
