package uk.ac.man.cs.eventlite.dao;

import java.util.Optional;


import uk.ac.man.cs.eventlite.entities.Venue;

public interface VenueService {

	public long count();
	
	public boolean existsById(long id);

	public Iterable<Venue> findAll();
	
	public Venue save(Venue venue);
	
	public Optional<Venue> findById(long id);
	
	public Iterable<Venue> getTopThreeVenuesByEvents();
	
	public Iterable<Venue> findAllByNameContainingIgnoreCase(String nameSearch);

	public void delete(Venue venue);
	
	public void deleteById(long id);

}
