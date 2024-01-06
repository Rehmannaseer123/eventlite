package uk.ac.man.cs.eventlite.dao;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import uk.ac.man.cs.eventlite.entities.Venue;

public interface VenueRepository extends CrudRepository<Venue, Long> {
	public long count();
	public boolean existsById(long id);
	public Iterable<Venue> findAll();
	public Venue save(Venue venue);
	public Optional<Venue> findById(long id);
	public void delete(Venue venue);
	public void deleteById(long id);

	
	public Iterable<Venue> findAllByNameContainingIgnoreCase(String nameSearch);
}
