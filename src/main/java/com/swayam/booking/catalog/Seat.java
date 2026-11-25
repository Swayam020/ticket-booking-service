package com.swayam.booking.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

// A seat belongs to one show, not to the venue: "is it free?" is a per-show question, and the
// status column is the state the double-booking race reads and writes
@Entity
@Table(name = "seat", uniqueConstraints = @UniqueConstraint(
		name = "uk_seat_show_row_number", columnNames = {"show_id", "row_label", "seat_number"}))
public class Seat {

	@Id
	@GeneratedValue
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "show_id", nullable = false)
	private Show show;

	// "row_label", not "row": ROW is a reserved word in MySQL
	@Column(name = "row_label", nullable = false, length = 5)
	private String rowLabel;

	@Column(name = "seat_number", nullable = false)
	private int seatNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private SeatStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Seat() {
	}

	public Seat(Show show, String rowLabel, int seatNumber) {
		this.show = show;
		this.rowLabel = rowLabel;
		this.seatNumber = seatNumber;
		this.status = SeatStatus.AVAILABLE;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public void markBooked() {
		status = SeatStatus.BOOKED;
	}

	public UUID getId() {
		return id;
	}

	public Show getShow() {
		return show;
	}

	public String getRowLabel() {
		return rowLabel;
	}

	public int getSeatNumber() {
		return seatNumber;
	}

	public SeatStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
