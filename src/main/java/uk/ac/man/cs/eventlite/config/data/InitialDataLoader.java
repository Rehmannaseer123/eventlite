package uk.ac.man.cs.eventlite.config.data;

import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

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
import uk.ac.man.cs.eventlite.dao.VenueServiceImpl;

@Configuration
@Profile("default")
public class InitialDataLoader {

	private final static Logger log = LoggerFactory.getLogger(InitialDataLoader.class);

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;
	
	@Bean
	public VenueService venueService()
	{
		return new VenueServiceImpl();
	}

	@Bean
	CommandLineRunner initDatabase() {
		return args -> {
			Venue testVenue = new Venue();
			testVenue.setName("Kilburn");
			testVenue.setStreetName("Oxford Road");
			testVenue.setPostcode("M13 9PL");
			testVenue.setCapacity(100);
			testVenue.setLatitude(53.467809996313605);
			testVenue.setLongitude(-2.2341463416711567);
			Venue testVenue1 = new Venue();
			testVenue1.setName("University Place");
			testVenue1.setStreetName("176 Oxford Road");
			testVenue1.setPostcode("M13 9PL");
			testVenue1.setCapacity(1000);
			testVenue1.setLatitude(53.46948325265133);
			testVenue1.setLongitude(-2.2342107146992354);
			Venue testVenue2 = new Venue();
			testVenue2.setName("Engineering Building");
			testVenue2.setStreetName("Booth Street E");
			testVenue2.setPostcode("M13");
			testVenue2.setCapacity(553);
			testVenue2.setLatitude(53.46939320412965);
			testVenue2.setLongitude(-2.2342427563079394);
			Venue testVenue3 = new Venue();
			testVenue3.setName("Sugden Centre");
			testVenue3.setStreetName("Oxford Road");
			testVenue3.setPostcode("M13 9PG");
			testVenue3.setCapacity(313);
			testVenue3.setLatitude(53.47129697390639);
			testVenue3.setLongitude(-2.236289843534859);
			Venue testVenue4 = new Venue();
			testVenue4 = new Venue();
			testVenue4.setName("Manchester Museum");
			testVenue4.setStreetName("Oxford Road");
			testVenue4.setPostcode("M13 9PL");
			testVenue4.setCapacity(325);
			testVenue4.setLatitude(53.46642258225204);
			testVenue4.setLongitude(-2.2342405300418893);
			
			if (venueService.count() > 0) {
				log.info("Database already populated with venues. Skipping venue initialization.");
			} else {
				venueService.save(testVenue);
				venueService.save(testVenue1);
				venueService.save(testVenue2);
				venueService.save(testVenue3);
				venueService.save(testVenue4);
			}

			if (eventService.count() > 0) {
				log.info("Database already populated with events. Skipping event initialization.");
			} else {
				Event event = new Event();
				event.setDate( LocalDate.now().plusDays(1));
				event.setTime(LocalTime.of(12, 00));
				event.setName("Software Engineering Lab");
				event.setDescription("Lab for software engineering week 7");
				event.setVenue(testVenue);
				eventService.save(event);
				Event event1 = new Event();
				event1.setDate(LocalDate.of(2023, 3, 13));
				event1.setTime(LocalTime.of(10, 00));
				event1.setName("Algorithms and Data Structures");
				event1.setDescription("Lecture for algorithms and data structures week 7");
				event1.setVenue(testVenue1);
				eventService.save(event1);
				Event event2 = new Event();
				event2.setDate(LocalDate.of(2023, 2, 18));
				event2.setTime(LocalTime.of(9, 00));
				event2.setName("Museum Exhibition");
				event2.setDescription("Manchester Museum re-opening exhibition");
				event2.setVenue(testVenue4);
				eventService.save(event2);
				Event event3 = new Event();
				event3.setDate(LocalDate.of(2023, 3, 24));
				event3.setTime(LocalTime.of(10, 00));
				event3.setName("Programming Languages And Paradigms");
				event3.setDescription("Live lecture about compilers week 5");
				event3.setVenue(testVenue1);
				eventService.save(event3);
				Event event4 = new Event();
				event4.setDate(LocalDate.of(2023, 3, 24));
				event4.setName("Careers Fair");
				event4.setDescription("Companies from across the UK coming to promote their businesses");
				event4.setVenue(testVenue2);
				eventService.save(event4);
				Event event5 = new Event();
				event5.setDate(LocalDate.of(2023, 3, 10));
				event5.setName("The Darren Huyton Sports Day");
				event5.setDescription("Sports day with activites including badminton, table tennis, dodgeball, basketball and football.");
				event5.setVenue(testVenue2);
				eventService.save(event5);
			}
		};
	}
}
