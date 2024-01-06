package uk.ac.man.cs.eventlite.dao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@Service
public class EventServiceImpl implements EventService {
	
	@Autowired
	private EventRepository eventrepository;

	private final static Logger log = LoggerFactory.getLogger(EventServiceImpl.class);


	@Override
	public long count() {
		return eventrepository.count();
	}
	
	@Override
	public boolean existsById(long id) {
		return eventrepository.existsById(id);
	}

	@Override
	public Iterable<Event> findAll() {
		return eventrepository.findAll(Sort.by(Sort.Direction.ASC, "date", "name"));
	}
	
	@Override
    public Iterable<Event> findPastEvents() {
		Iterable<Event> pastEvents = eventrepository.findByDateLessThanEqualOrderByDateDescNameAsc(LocalDate.now());
		return StreamSupport.stream(pastEvents.spliterator(), false)
	            .filter(event -> {
	                if (event.getDate().isEqual(LocalDate.now())) {
	                    if (event.getTime() == null) {
	                        return false;
	                    }
	                    return event.getTime().isBefore(LocalTime.now());
	                }
	                return event.getDate().isBefore(LocalDate.now());
	            })
	            .collect(Collectors.toList());
    }

    @Override
    public Iterable<Event> findUpcomingEvents() {
    	Iterable<Event> upcomingEvents = eventrepository.findByDateGreaterThanEqualOrderByDateAscNameAsc(LocalDate.now());
    	return StreamSupport.stream(upcomingEvents.spliterator(), false)
                .filter(event -> {
                    if (event.getDate().isEqual(LocalDate.now())) {
                        if (event.getTime() == null) {
                            return true;
                        }
                        return event.getTime().isAfter(LocalTime.now());
                    }
                    return event.getDate().isAfter(LocalDate.now());
                })
                .collect(Collectors.toList());
    }
	
	@Override
	public Event save(Event event) {
		return eventrepository.save(event);
	}
	
	@Override
	public Optional<Event> findById(long id){
		return eventrepository.findById(id);
	}

	@Override
	public void delete(Event event) {
		eventrepository.delete(event);
	}

	@Override
	public void deleteById(long id) {
		eventrepository.deleteById(id);
	}

	@Override
	public Iterable<Event> findAllByNameContainingIgnoreCase(String nameSearch){
		return eventrepository.findAllByNameContainingIgnoreCase(nameSearch);
	}
	
	@Override
	public Iterable<Event> findPastEventsByNameContainingIgnoreCase(String nameSearch){
		Iterable<Event> pastEventsByNameContaining = eventrepository.findAllByNameContainingIgnoreCaseAndDateLessThanEqualOrderByDateDescNameAsc(nameSearch, LocalDate.now());
		return StreamSupport.stream(pastEventsByNameContaining.spliterator(), false)
	            .filter(event -> {
	                if (event.getDate().isEqual(LocalDate.now())) {
	                    if (event.getTime() == null) {
	                        return false;
	                    }
	                    return event.getTime().isBefore(LocalTime.now());
	                }
	                return event.getDate().isBefore(LocalDate.now());
	            })
	            .collect(Collectors.toList());
	}
	
	@Override
	public Iterable<Event> findUpcomingEventsByNameContainingIgnoreCase(String nameSearch){
		Iterable<Event> upcomingEventsByNameContaining = eventrepository.findAllByNameContainingIgnoreCaseAndDateGreaterThanEqualOrderByDateAscNameAsc(nameSearch, LocalDate.now());
		return StreamSupport.stream(upcomingEventsByNameContaining.spliterator(), false)
                .filter(event -> {
                    if (event.getDate().isEqual(LocalDate.now())) {
                        if (event.getTime() == null) {
                            return true;
                        }
                        return event.getTime().isAfter(LocalTime.now());
                    }
                    return event.getDate().isAfter(LocalDate.now());
                })
                .collect(Collectors.toList());
	}
	
	@Override
	public Iterable<Event> findUpcomingEventsByVenue(Venue venue){
		return eventrepository.findAllByVenueAndDateGreaterThanEqualOrderByDateAscNameAsc(venue,LocalDate.now());
	}
	
	@Override
	public Iterable<Event> findPastEventsByVenue(Venue venue){
		return eventrepository.findAllByVenueAndDateLessThanOrderByDateAscNameAsc(venue,LocalDate.now());
	}
	
	@Override
	public Iterable<Event> findNextThreeEvents(){
		return StreamSupport.stream(findUpcomingEvents().spliterator(), false).limit(3).collect(Collectors.toList()); 
	}
	

}
