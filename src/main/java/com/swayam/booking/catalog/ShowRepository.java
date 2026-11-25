package com.swayam.booking.catalog;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShowRepository extends JpaRepository<Show, UUID> {

	// fetches the venue in the same query: listing N shows would otherwise cost N extra selects
	@EntityGraph(attributePaths = "venue")
	List<Show> findAllByOrderByStartsAtAsc();

}
