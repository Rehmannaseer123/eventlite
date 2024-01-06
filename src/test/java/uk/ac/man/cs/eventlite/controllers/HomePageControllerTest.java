package uk.ac.man.cs.eventlite.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@WebMvcTest(HomePageController.class)
public class HomePageControllerTest {

	@Autowired
	private MockMvc mvc;

	@Mock
	private Event event;

	@Mock
	private Venue venue;

	@MockBean
	private EventService eventService;

	@MockBean
	private VenueService venueService;

	@Test
	public void getIndexWhenNoEventsAndVenues() throws Exception {
		
        Iterable<Event> nextThreeEvents = new ArrayList<>();
        Iterable<Venue> threeTopVenues = new ArrayList<>();

        when(eventService.findNextThreeEvents()).thenReturn(nextThreeEvents);
        when(venueService.getTopThreeVenuesByEvents()).thenReturn(threeTopVenues);

        mvc.perform(get("/home"))
            .andExpect(status().isOk())
            .andExpect(view().name("home/index"))
            .andExpect(model().attribute("nextThreeEvents", nextThreeEvents))
        	.andExpect(model().attribute("topThreeVenues", threeTopVenues));
        
	}

	@Test
	public void getIndexWithEventsAndVenues() throws Exception {
		
		when(venueService.getTopThreeVenuesByEvents()).thenReturn(Collections.<Venue>singletonList(venue));
		when(eventService.findNextThreeEvents()).thenReturn(Collections.<Event>singletonList(event));
		when(event.getVenue()).thenReturn(new Venue());
		
		mvc.perform(get("/home")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("home/index"))
				.andExpect(handler().methodName("home"));

		verify(venueService).getTopThreeVenuesByEvents();
		verify(eventService).findNextThreeEvents();

	}
	
}
