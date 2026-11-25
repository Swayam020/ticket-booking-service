package com.swayam.booking.catalog;

import com.swayam.booking.NotFoundException;
import com.swayam.booking.api.Dtos.CreateShowRequest;
import com.swayam.booking.api.Dtos.CreateVenueRequest;
import com.swayam.booking.api.Dtos.SeatResponse;
import com.swayam.booking.api.Dtos.ShowResponse;
import com.swayam.booking.api.Dtos.VenueResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CatalogService {

	private final VenueRepository venues;
	private final ShowRepository shows;
	private final SeatRepository seats;

	public CatalogService(VenueRepository venues, ShowRepository shows, SeatRepository seats) {
		this.venues = venues;
		this.shows = shows;
		this.seats = seats;
	}

	@Transactional
	public VenueResponse createVenue(CreateVenueRequest request) {
		return toResponse(venues.save(new Venue(request.name(), request.city())));
	}

	@Transactional
	public ShowResponse createShow(CreateShowRequest request) {
		Venue venue = venues.findById(request.venueId())
				.orElseThrow(() -> new NotFoundException("venue not found"));
		Show show = shows.save(new Show(venue, request.title(), request.startsAt()));
		List<Seat> grid = new ArrayList<>();
		for (int row = 0; row < request.rows(); row++) {
			String label = String.valueOf((char) ('A' + row));
			for (int number = 1; number <= request.seatsPerRow(); number++) {
				grid.add(new Seat(show, label, number));
			}
		}
		seats.saveAll(grid);
		return toResponse(show);
	}

	@Transactional(readOnly = true)
	public List<ShowResponse> listShows() {
		return shows.findAllByOrderByStartsAtAsc().stream().map(CatalogService::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public List<SeatResponse> seatsOf(UUID showId) {
		if (!shows.existsById(showId)) {
			throw new NotFoundException("show not found");
		}
		return seats.findByShowIdOrderByRowLabelAscSeatNumberAsc(showId).stream()
				.map(seat -> new SeatResponse(seat.getId(), seat.getRowLabel(), seat.getSeatNumber(), seat.getStatus()))
				.toList();
	}

	private static VenueResponse toResponse(Venue venue) {
		return new VenueResponse(venue.getId(), venue.getName(), venue.getCity());
	}

	private static ShowResponse toResponse(Show show) {
		return new ShowResponse(show.getId(), show.getTitle(), show.getStartsAt(), toResponse(show.getVenue()));
	}

}
