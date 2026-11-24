package com.swayam.booking.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "venue")
public class Venue {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 100)
	private String city;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Venue() {
	}

	public Venue(String name, String city) {
		this.name = name;
		this.city = city;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getCity() {
		return city;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
