package com.swayam.booking.reservation;

import com.swayam.booking.account.AppUser;
import com.swayam.booking.catalog.Seat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

// seat_id is deliberately NOT unique: a unique key would make the database reject the second
// booking, and the double-booking race this project reproduces (M4) could not happen
@Entity
@Table(name = "booking")
public class Booking {

	@Id
	@GeneratedValue
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seat_id", nullable = false)
	private Seat seat;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser user;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Booking() {
	}

	public Booking(Seat seat, AppUser user) {
		this.seat = seat;
		this.user = user;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public Seat getSeat() {
		return seat;
	}

	public AppUser getUser() {
		return user;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
