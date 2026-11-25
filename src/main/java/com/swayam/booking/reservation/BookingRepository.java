package com.swayam.booking.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

	long countBySeatId(UUID seatId);

	Optional<Booking> findByIdAndUserId(UUID id, UUID userId);

}
