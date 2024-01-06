package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import uk.ac.man.cs.eventlite.mastodon.MastodonService;
import uk.ac.man.cs.eventlite.mastodon.Post;

@ExtendWith(SpringExtension.class)
@WebMvcTest(EventsController.class)
@Import(Security.class)
public class EventsControllerTest {

	public static MastodonService testInstance = MastodonService.getInstance();
	
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
	public void getIndexWhenNoEvents() throws Exception {
		
		Iterable<Event> pastEvents = new ArrayList<>();
        Iterable<Event> upcomingEvents = new ArrayList<>();

        when(eventService.findPastEvents()).thenReturn(pastEvents);
        when(eventService.findUpcomingEvents()).thenReturn(upcomingEvents);

        mvc.perform(get("/events"))
            .andExpect(status().isOk())
            .andExpect(view().name("events/index"))
            .andExpect(model().attribute("pastEvents", pastEvents))
            .andExpect(model().attribute("upcomingEvents", upcomingEvents));

	}

	@Test
	public void getIndexWithEvents() throws Exception {
		when(eventService.findPastEvents()).thenReturn(Collections.<Event>singletonList(event));
		when(eventService.findUpcomingEvents()).thenReturn(Collections.<Event>singletonList(event));
		when(event.getVenue()).thenReturn(new Venue());
		
		mvc.perform(get("/events").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("events/index")).andExpect(handler().methodName("getAllEvents"));

		verify(eventService).findPastEvents();
		verify(eventService).findUpcomingEvents();
	}

	@Test
	public void getEventNotFound() throws Exception {
		mvc.perform(get("/events/99").accept(MediaType.TEXT_HTML)).andExpect(status().isNotFound())
				.andExpect(view().name("events/not_found")).andExpect(handler().methodName("getEvent"));
	}
	
	@Test
	public void getEvent() throws Exception {
		when(event.getVenue()).thenReturn(new Venue());
		when(eventService.findById(1)).thenReturn(Optional.of(event));

		mvc.perform(get("/events/1").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("events/detail")).andExpect(handler().methodName("getEvent"));

		verify(event,atLeastOnce()).getVenue();
		verify(eventService).findById(1);
	}
	
	@Test
	public void getEventSendPost() throws Exception {
		when(event.getVenue()).thenReturn(new Venue());
		when(eventService.findById(1)).thenReturn(Optional.of(event));

		mvc.perform(post("/events/1").with(user("Attendee").roles(Security.ATTENDEE_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("postContent", "post test")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/detail")).andExpect(handler().methodName("getEventSendPost"));
		
		// call the readHome method
		try {
			Thread.sleep(500);
		} catch (Exception e) {}
		Iterable<Post> posts = testInstance.readHome();
		try {
			Thread.sleep(500);
		} catch (Exception e) {}
		// delete test post
		testInstance.deleteLastStatus();
		
		// check test post published correctly
		Iterator<Post> it = posts.iterator();
		assertThat(it.next().getContent(), equalTo("post test"));
	}
	
	@Test
	public void getEventName() throws Exception {
		when(event.getVenue()).thenReturn(new Venue());
		when(eventService.findById(1)).thenReturn(Optional.of(event));

		mvc.perform(get("/events/1?name=Software").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("events/detail")).andExpect(handler().methodName("getEvent"));
		
		verify(event,atLeastOnce()).getVenue();
		verify(eventService).findById(1);

	}
	
	@Test
	public void searchEventByNameWhenNameIsEmpty() throws Exception {

        when(eventService.findPastEventsByNameContainingIgnoreCase("")).thenReturn(Collections.<Event>emptyList());
        when(eventService.findUpcomingEventsByNameContainingIgnoreCase("")).thenReturn(Collections.<Event>emptyList());
		
		mvc.perform(get("/events/search").accept(MediaType.TEXT_HTML).param("nameSearch", ""))
		.andExpect(status().isOk()).andExpect(view().name("events/search"))
		.andExpect(model().attribute("pastEvents", Collections.<Event>emptyList()))
        .andExpect(model().attribute("upcomingEvents", Collections.<Event>emptyList()));
		
		verify(eventService,atLeastOnce()).findPastEventsByNameContainingIgnoreCase("");
		verify(eventService,atLeastOnce()).findUpcomingEventsByNameContainingIgnoreCase("");
	
	}
	
	@Test
	public void searchEventByNameWhenNameIsNotEmpty() throws Exception {
		
		String searchTerm = "Data";
		
		when(eventService.findPastEventsByNameContainingIgnoreCase(searchTerm)).thenReturn(Collections.<Event>emptyList());
		when(eventService.findUpcomingEventsByNameContainingIgnoreCase(searchTerm)).thenReturn(Collections.<Event>emptyList());
		
		mvc.perform(get("/events/search").accept(MediaType.TEXT_HTML).param("nameSearch", searchTerm))
		.andExpect(status().isOk()).andExpect(view().name("events/search"))
		.andExpect(model().attribute("pastEvents", Collections.<Event>emptyList()))
        .andExpect(model().attribute("upcomingEvents", Collections.<Event>emptyList()));
		
		verify(eventService,atLeastOnce()).findPastEventsByNameContainingIgnoreCase(searchTerm);
		verify(eventService,atLeastOnce()).findUpcomingEventsByNameContainingIgnoreCase(searchTerm);
	
	}
	
	@Test
	public void deleteEvent() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(delete("/events/1").with(user("Organiser").roles(Security.ORG_ROLE)).accept(MediaType.TEXT_HTML).with(csrf()))
		.andExpect(status().isFound()).andExpect(view().name("redirect:/events"))
				.andExpect(handler().methodName("deleteEvent")).andExpect(flash().attributeExists("ok_message"));

	}
	
	@Test
	public void deleteEventNotFound() throws Exception {
		when(eventService.existsById(1)).thenReturn(false);

		mvc.perform(delete("/events/1").with(user("Organiser").roles(Security.ORG_ROLE)).accept(MediaType.TEXT_HTML).with(csrf()))
		.andExpect(status().isNotFound()).andExpect(view().name("events/not_found"))
				.andExpect(handler().methodName("deleteEvent"));

		verify(eventService, never()).deleteById(1);
	}
	
	@Test
	public void postNewEvent() throws Exception {
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		Venue venue = new Venue();
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(eventService.save(any(Event.class))).then(returnsFirstArg());

		mvc.perform(post("/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test event").param("description", "Test description").param("date", LocalDate.now().plusDays(1).toString()).param("time", "12:00").param("venueId", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/events")).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeExists("ok_message"));

		verify(eventService).save(arg.capture());
		assertThat("Test event", equalTo(arg.getValue().getName()));
		assertThat("Test description", equalTo(arg.getValue().getDescription()));
		assertThat(LocalDate.now().plusDays(1), equalTo(arg.getValue().getDate()));
		assertThat("12:00", equalTo(arg.getValue().getTime().toString()));
		assertThat(venue, equalTo(arg.getValue().getVenue()));
	}
	
	@Test
	public void updateEvent() throws Exception {
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		Venue venue = new Venue();
		Event event = new Event();
		event.setId(1);
		event.setName("test");
		event.setDescription("description");
		event.setDate(LocalDate.parse("1970-01-01"));
		event.setTime(LocalTime.parse("12:00"));
		event.setVenue(venue);
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(eventService.save(any(Event.class))).then(returnsFirstArg());

		mvc.perform(post("/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test event").param("description", "Test description").param("date", LocalDate.now().plusDays(1).toString()).param("time", "12:00").param("venueId", "1").param("id", event.getId()+"")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/events/"+event.getId())).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeExists("ok_message"));

		verify(eventService).save(arg.capture());
		assertThat("Test event", equalTo(arg.getValue().getName()));
		assertThat("Test description", equalTo(arg.getValue().getDescription()));
		assertThat(LocalDate.now().plusDays(1), equalTo(arg.getValue().getDate()));
		assertThat("12:00", equalTo(arg.getValue().getTime().toString()));
		assertThat(venue, equalTo(arg.getValue().getVenue()));
	}
	
	@Test
	public void testPastDate() throws Exception
	{	
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		Venue venue = new Venue();
		Event event = new Event();
		LocalDate pastDate = LocalDate.now().minusDays(1);
		
		event.setName("test");
		event.setVenue(venue);
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(eventService.findById(any(Long.class))).thenReturn(Optional.of(event));
		when(eventService.save(any(Event.class))).then(returnsFirstArg());
		
		mvc.perform(post("/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test event").param("date", pastDate.toString()).param("venueId", "1").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/update")).andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeCount(0));
		
		mvc.perform(post("/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test event").param("date", pastDate.toString()).param("venueId", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/new")).andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(arg.capture());
	}
	
	@Test
	public void testPresentDate() throws Exception
	{	
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		Venue venue = new Venue();
		Event event = new Event();
		LocalDate currentDate = LocalDate.now();
		
		event.setName("test");
		event.setVenue(venue);
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(eventService.findById(any(Long.class))).thenReturn(Optional.of(event));
		when(eventService.save(any(Event.class))).then(returnsFirstArg());
		
		mvc.perform(post("/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test event").param("date", currentDate.toString()).param("venueId", "1").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/update")).andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeCount(0));
		
		mvc.perform(post("/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test event").param("date", currentDate.toString()).param("venueId", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/new")).andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(arg.capture());
	}
	
	@Test
	public void testOptionalDetails() throws Exception
	{	
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		Venue venue = new Venue();
		Event event = new Event();
		LocalDate futureDate = LocalDate.now().plusDays(1);
		
		event.setId(1);
		event.setName("test");
		event.setVenue(venue);
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(eventService.findById(any(Long.class))).thenReturn(Optional.of(event));
		when(eventService.save(any(Event.class))).then(returnsFirstArg());
		
		mvc.perform(post("/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test event").param("date", futureDate.toString()).param("venueId", "1").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf()))
				.andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/events/"+event.getId())).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeExists("ok_message"));
				
		mvc.perform(post("/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test event").param("date", futureDate.toString()).param("venueId", "1")
				.accept(MediaType.TEXT_HTML).with(csrf()))
				.andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/events")).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeExists("ok_message"));
		
		verify(eventService, times(2)).save(arg.capture());
	}
	
	@Test
	public void testMissingName() throws Exception
	{	
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		Venue venue = new Venue();
		Event event = new Event();
		LocalDate currentDate = LocalDate.now().plusDays(1);
		
		event.setVenue(venue);
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(eventService.findById(any(Long.class))).thenReturn(Optional.of(event));
		when(eventService.save(any(Event.class))).then(returnsFirstArg());
		
		mvc.perform(post("/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("date", currentDate.toString()).param("venueId", "1").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/update")).andExpect(model().attributeHasFieldErrors("event", "name"))
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeCount(0));
		
		mvc.perform(post("/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("date", currentDate.toString()).param("venueId", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/new")).andExpect(model().attributeHasFieldErrors("event", "name"))
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(arg.capture());
	}
	
	@Test
	public void testMissingDate() throws Exception
	{	
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		Venue venue = new Venue();
		Event event = new Event();
		
		event.setName("test");
		event.setVenue(venue);
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(eventService.findById(any(Long.class))).thenReturn(Optional.of(event));
		when(eventService.save(any(Event.class))).then(returnsFirstArg());
		
		mvc.perform(post("/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "test place").param("venueId", "1").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/update")).andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeCount(0));
		
		mvc.perform(post("/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "test place").param("venueId", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/new")).andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(arg.capture());
	}
	
	@Test
	public void testMissingVenue() throws Exception
	{	
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		Venue venue = new Venue();
		Event event = new Event();
		LocalDate currentDate = LocalDate.now().plusDays(1);
		
		
		event.setName("test");
		event.setVenue(venue);
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(eventService.findById(any(Long.class))).thenReturn(Optional.of(event));
		when(eventService.save(any(Event.class))).then(returnsFirstArg());
		
		mvc.perform(post("/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("date", currentDate.toString()).param("name", "test place").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/update")).andExpect(model().attributeHasFieldErrors("event", "venueId"))
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeCount(0));
		
		mvc.perform(post("/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("date", currentDate.toString()).param("name", "test place")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/new")).andExpect(model().attributeHasFieldErrors("event", "venueId"))
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(arg.capture());
	}
	
	@Test
	public void testNameTooLong() throws Exception
	{	
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		Venue venue = new Venue();
		Event event = new Event();
		LocalDate currentDate = LocalDate.now().plusDays(1);
		String longName = new String(new char[257]).replace("\0", "1");

		event.setName(longName);
		event.setVenue(venue);
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(eventService.findById(any(Long.class))).thenReturn(Optional.of(event));
		when(eventService.save(any(Event.class))).then(returnsFirstArg());
		
		mvc.perform(post("/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("date", currentDate.toString()).param("name", longName).param("id", "1").param("venueId", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/update")).andExpect(model().attributeHasFieldErrors("event", "name"))
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeCount(0));
		
		mvc.perform(post("/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("date", currentDate.toString()).param("name", longName).param("venueId", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/new")).andExpect(model().attributeHasFieldErrors("event", "name"))
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(arg.capture());
	}
	
	@Test
	public void testDescriptionTooLong() throws Exception
	{	
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		Venue venue = new Venue();
		Event event = new Event();
		LocalDate currentDate = LocalDate.now().plusDays(1);
		String longDescr = new String(new char[501]).replace("\0", "1");

		event.setName("test");
		event.setDescription(longDescr);
		event.setVenue(venue);
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(eventService.findById(any(Long.class))).thenReturn(Optional.of(event));
		when(eventService.save(any(Event.class))).then(returnsFirstArg());
		
		mvc.perform(post("/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("date", currentDate.toString()).param("name", "test").param("id", "1").param("venueId", "1").param("description", longDescr)
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/update")).andExpect(model().attributeHasFieldErrors("event", "description"))
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeCount(0));
		
		mvc.perform(post("/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("date", currentDate.toString()).param("name", "test").param("venueId", "1").param("description", longDescr)
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/new")).andExpect(model().attributeHasFieldErrors("event", "description"))
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(arg.capture());
	}
	
	
	
	
}
