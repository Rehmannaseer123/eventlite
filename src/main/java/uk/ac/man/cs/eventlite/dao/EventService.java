package uk.ac.man.cs.eventlite.dao;

import java.util.Optional;

import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

public interface EventService {

	public long count();
	
	public boolean existsById(long id);

	public Iterable<Event> findAll();
	
	public Event save(Event envent);
	
	public Optional<Event> findById(long id);
	
	public Iterable<Event> findUpcomingEvents();
	
	public Iterable<Event> findPastEvents();
	
	public Iterable<Event> findNextThreeEvents();

	public Iterable<Event> findAllByNameContainingIgnoreCase(String nameSearch);
	
	public Iterable<Event> findPastEventsByNameContainingIgnoreCase(String nameSearch);
	
	public Iterable<Event> findUpcomingEventsByNameContainingIgnoreCase(String nameSearch);
	
	public void delete(Event event);
	
	public void deleteById(long id);
	
	public Iterable<Event> findUpcomingEventsByVenue(Venue venue);
	
	public Iterable<Event> findPastEventsByVenue(Venue venue);
	
	

}
