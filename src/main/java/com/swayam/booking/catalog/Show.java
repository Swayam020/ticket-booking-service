package com.swayam.booking.catalog;

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

// "shows", not "show": SHOW is a reserved word in MySQL
@Entity
@Table(name = "shows")
public class Show {

	@Id
	@GeneratedValue
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "venue_id", nullable = false)
	private Venue venue;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(name = "starts_at", nullable = false)
	private Instant startsAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Show() {
	}

	public Show(Venue venue, String title, Instant startsAt) {
		this.venue = venue;
		this.title = title;
		this.startsAt = startsAt;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public Venue getVenue() {
		return venue;
	}

	public String getTitle() {
		return title;
	}

	public Instant getStartsAt() {
		return startsAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
