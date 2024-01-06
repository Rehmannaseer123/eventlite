package uk.ac.man.cs.eventlite.config.data;

import java.time.LocalDate;
import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@Configuration
@Profile("test")
public class TestDataLoader {

	private final static Logger log = LoggerFactory.getLogger(TestDataLoader.class);

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	@Bean
	CommandLineRunner initDatabase() {
		return args -> {
			
			Venue testVenue = new Venue();
			testVenue.setCapacity(100);
			testVenue.setName("Kilburn");
			venueService.save(testVenue);
			
			Event event = new Event();
			event.setDate(LocalDate.now());
			event.setTime(LocalTime.now());
			event.setName("Software Engineering Lecture");
			event.setDescription("Lecture for software engineering week 3");
			event.setVenue(testVenue);
			eventService.save(event);
			Event event1 = new Event();
			event1.setDate(LocalDate.now());
			event1.setTime(LocalTime.now());
			event1.setName("Software Engineering Lecture 1");
			event1.setDescription("Lecture for software engineering week 4");
			event1.setVenue(testVenue);
			eventService.save(event1);
			Event event2 = new Event();
			event2.setDate(LocalDate.now());
			event2.setTime(LocalTime.now());
			event2.setName("Software Engineering Lecture 2");
			event2.setDescription("Lecture for software engineering week 5");
			event2.setVenue(testVenue);
			eventService.save(event2);
			
		};
	}
}
