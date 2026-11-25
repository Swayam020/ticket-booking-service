package com.swayam.booking.api;

import com.swayam.booking.api.Dtos.CreateShowRequest;
import com.swayam.booking.api.Dtos.CreateVenueRequest;
import com.swayam.booking.api.Dtos.SeatResponse;
import com.swayam.booking.api.Dtos.ShowResponse;
import com.swayam.booking.api.Dtos.VenueResponse;
import com.swayam.booking.catalog.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
class CatalogController {

	private final CatalogService catalog;

	CatalogController(CatalogService catalog) {
		this.catalog = catalog;
	}

	@PostMapping("/venues")
	@ResponseStatus(HttpStatus.CREATED)
	VenueResponse createVenue(@Valid @RequestBody CreateVenueRequest request) {
		return catalog.createVenue(request);
	}

	@PostMapping("/shows")
	@ResponseStatus(HttpStatus.CREATED)
	ShowResponse createShow(@Valid @RequestBody CreateShowRequest request) {
		return catalog.createShow(request);
	}

	@GetMapping("/shows")
	List<ShowResponse> listShows() {
		return catalog.listShows();
	}

	@GetMapping("/shows/{showId}/seats")
	List<SeatResponse> seats(@PathVariable UUID showId) {
		return catalog.seatsOf(showId);
	}

}
