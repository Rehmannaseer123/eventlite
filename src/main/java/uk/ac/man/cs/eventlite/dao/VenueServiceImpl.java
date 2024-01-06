package uk.ac.man.cs.eventlite.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@Service
public class VenueServiceImpl implements VenueService {

	@Autowired
	private VenueRepository venueRepository;
	
	private final static Logger log = LoggerFactory.getLogger(VenueServiceImpl.class);

	private final static String DATA = "data/venues.json";

	@Override
	public long count() {
		return venueRepository.count();
	}
	
	@Override
	public boolean existsById(long id) {
		return venueRepository.existsById(id);
	}

	@Override
	public Iterable<Venue> findAll() {
		return venueRepository.findAll();
	}
	
	@Override
	public Venue save(Venue venue) {
		return venueRepository.save(venue);
	}
	
	@Override
	public Optional<Venue> findById(long id) {
		return venueRepository.findById(id);
	}
	
	public Iterable<Venue> getTopThreeVenuesByEvents() {
		Iterable<Venue> venues = venueRepository.findAll();
		HashMap<Venue, Integer> countOfEventsInVenues = new HashMap<>();
		for (Venue v : venues) countOfEventsInVenues.put(v, v.getEvents().size());
		ArrayList<HashMap.Entry<Venue, Integer>> sortVenuesByEventCount = new ArrayList<>(countOfEventsInVenues.entrySet());
		sortVenuesByEventCount.sort((v1, v2) -> v2.getValue().compareTo(v1.getValue()));
		ArrayList<Venue> threeTopVenues = new ArrayList<>();
		for (HashMap.Entry<Venue, Integer> venue : sortVenuesByEventCount) {
			if(threeTopVenues.size() == 3) break;
			threeTopVenues.add(venue.getKey());
		}
		return threeTopVenues;
	}
	
	@Override
	public Iterable<Venue> findAllByNameContainingIgnoreCase(String nameSearch){
		return venueRepository.findAllByNameContainingIgnoreCase(nameSearch);
	}

	@Override
	public void delete(Venue venue) {
		venueRepository.delete(venue);
	}

	@Override
	public void deleteById(long id) {
		venueRepository.deleteById(id);
	}
}
