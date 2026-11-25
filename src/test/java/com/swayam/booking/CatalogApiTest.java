package com.swayam.booking;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CatalogApiTest {

	@Autowired MockMvc mvc;

	private String postJson(String path, String body) throws Exception {
		return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
	}

	private String createVenue() throws Exception {
		String venue = postJson("/api/venues", """
				{"name": "Grand Hall", "city": "Mumbai"}""");
		return JsonPath.read(venue, "$.id");
	}

	private String createShow(String title, int rows, int seatsPerRow) throws Exception {
		String show = postJson("/api/shows", """
				{"venueId": "%s", "title": "%s", "startsAt": "2026-12-31T14:30:00Z", "rows": %d, "seatsPerRow": %d}"""
				.formatted(createVenue(), title, rows, seatsPerRow));
		return JsonPath.read(show, "$.id");
	}

	@Test
	void createdShowGetsAFullSeatGrid() throws Exception {
		String showId = createShow("Hamlet", 2, 3);

		mvc.perform(get("/api/shows/{id}/seats", showId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(6)))
				.andExpect(jsonPath("$[*].row", contains("A", "A", "A", "B", "B", "B")))
				.andExpect(jsonPath("$[*].number", contains(1, 2, 3, 1, 2, 3)))
				.andExpect(jsonPath("$[*].status", everyItem(is("AVAILABLE"))));
	}

	@Test
	void showListIncludesItsVenue() throws Exception {
		String showId = createShow("Macbeth", 1, 1);

		// other tests share this database, so find this show by id instead of counting the list
		mvc.perform(get("/api/shows"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '%s')].title".formatted(showId), contains("Macbeth")))
				.andExpect(jsonPath("$[?(@.id == '%s')].startsAt".formatted(showId), contains("2026-12-31T14:30:00Z")))
				.andExpect(jsonPath("$[?(@.id == '%s')].venue.name".formatted(showId), contains("Grand Hall")))
				.andExpect(jsonPath("$[?(@.id == '%s')].venue.city".formatted(showId), contains("Mumbai")));
	}

	@Test
	void seatsOfAnUnknownShowAre404() throws Exception {
		mvc.perform(get("/api/shows/{id}/seats", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value("show not found"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void showAtAnUnknownVenueIs404() throws Exception {
		mvc.perform(post("/api/shows").contentType(MediaType.APPLICATION_JSON).content("""
						{"venueId": "%s", "title": "Othello", "startsAt": "2026-12-31T14:30:00Z", "rows": 1, "seatsPerRow": 1}"""
						.formatted(UUID.randomUUID())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("venue not found"));
	}

	@Test
	void invalidShowNamesEveryBadField() throws Exception {
		mvc.perform(post("/api/shows").contentType(MediaType.APPLICATION_JSON).content("""
						{"venueId": "%s", "title": " ", "startsAt": "2026-12-31T14:30:00Z", "rows": 0, "seatsPerRow": 1}"""
						.formatted(UUID.randomUUID())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message", containsString("rows ")))
				.andExpect(jsonPath("$.message", containsString("; title ")));
	}

	@Test
	void aMalformedIdIs400() throws Exception {
		mvc.perform(get("/api/shows/not-a-uuid/seats"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("invalid id"));
	}

}
