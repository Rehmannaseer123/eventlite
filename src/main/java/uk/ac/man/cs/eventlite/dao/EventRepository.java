package uk.ac.man.cs.eventlite.dao;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

public interface EventRepository extends CrudRepository<Event, Long> {
	public long count();
	public boolean existsById(long id);
	public Iterable<Event> findAll();
	public Event save(Event event);
	public Optional<Event> findById(long id);
	public void delete(Event event);
	public void deleteById(long id);
	Iterable<Event> findAll(Sort sort);
	Iterable<Event> findByDateGreaterThanEqualOrderByDateAscNameAsc(LocalDate date);
	Iterable<Event> findByDateLessThanEqualOrderByDateDescNameAsc(LocalDate date);
	Iterable<Event> findAllByNameContainingIgnoreCase(String nameSearch);
	Iterable<Event> findAllByNameContainingIgnoreCaseAndDateLessThanEqualOrderByDateDescNameAsc(String nameSearch,LocalDate date);
	Iterable<Event> findAllByNameContainingIgnoreCaseAndDateGreaterThanEqualOrderByDateAscNameAsc(String nameSearch,LocalDate date);
	Iterable<Event> findAllByVenueAndDateGreaterThanEqualOrderByDateAscNameAsc(Venue venue, LocalDate date);
	Iterable<Event> findAllByVenueAndDateLessThanOrderByDateAscNameAsc(Venue venue, LocalDate date);
}
