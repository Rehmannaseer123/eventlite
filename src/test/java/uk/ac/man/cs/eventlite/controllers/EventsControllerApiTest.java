package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;
import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@WebMvcTest(EventsControllerApi.class)
@Import({ Security.class, EventModelAssembler.class })
public class EventsControllerApiTest {

	@Autowired
	private MockMvc mvc;

	@MockBean
	private EventService eventService;
	
	@MockBean
	private VenueService venueService;


	@Test
	public void getIndexWhenNoEvents() throws Exception {
		when(eventService.findAll()).thenReturn(Collections.<Event>emptyList());

		mvc.perform(get("/api/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllEvents")).andExpect(jsonPath("$.length()", equalTo(1)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/events")));

		verify(eventService).findAll();
	}

	@Test
	public void getIndexWithEvents() throws Exception {
		Event e = new Event();
		e.setId(0);
		e.setName("Event");
		e.setDate(LocalDate.now());
		e.setTime(LocalTime.now());
		e.setVenue(new Venue());
		when(eventService.findAll()).thenReturn(Collections.<Event>singletonList(e));

		mvc.perform(get("/api/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllEvents")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/events")))
				.andExpect(jsonPath("$._embedded.events.length()", equalTo(1)));

		verify(eventService).findAll();
	}

	@Test
	public void getEventNotFound() throws Exception {
		mvc.perform(get("/api/events/99").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("event 99"))).andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("getEvent"));
	}
	
	@Test
	public void getEvent() throws Exception {
		Event e = new Event();
		e.setId(0);
		e.setName("Event");
		e.setDate(LocalDate.now());
		e.setTime(LocalTime.now());
		e.setDescription("A event");
		Venue v = new Venue();
		v.setId(1);
		e.setVenue(v);

		when(eventService.findById(0)).thenReturn(Optional.of(e));
		mvc.perform(get("/api/events/0").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getEvent"))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/events/0")))
				.andExpect(jsonPath("$._links.event.href", endsWith("/api/events/0")))
				.andExpect(jsonPath("$._links.venue.href", endsWith("/api/venues/1")))
				.andExpect(jsonPath("$.id", equalTo(0)));
		
		verify(eventService).findById(0);
	}
	
	@Test
	public void searchEventByNameWhenNameIsNotEmpty() throws Exception {
		Event e = new Event();
		e.setId(0);
		e.setName("Event");
		e.setDate(LocalDate.now());
		e.setTime(LocalTime.now());
		e.setVenue(new Venue());
		Event e1 = new Event();
		e1.setId(0);
		e1.setName("Test");
		e1.setDate(LocalDate.now());
		e1.setTime(LocalTime.now());
		e1.setVenue(new Venue());
		when(eventService.findAllByNameContainingIgnoreCase("Eve")).thenReturn(Collections.<Event>singletonList(e));

		mvc.perform(get("/api/events/search").accept(MediaType.APPLICATION_JSON).param("nameSearch", "Eve")).andExpect(status().isOk())
				.andExpect(handler().methodName("getEventsByNameContaining")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/events/search?nameSearch=Eve")))
				.andExpect(jsonPath("$._embedded.events.length()", equalTo(1)));

		verify(eventService).findAllByNameContainingIgnoreCase("Eve");
	}
	
	@Test
	public void deleteEvent() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		mvc.perform(delete("/api/events/1").with(user("Organiser").roles(Security.ORG_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent())
				.andExpect(content().string(""))
				.andExpect(handler().methodName("deleteEvent"));

		verify(eventService).deleteById(1);
	}
	
	@Test
	public void deleteEventNotFound() throws Exception {
		when(eventService.existsById(1)).thenReturn(false);

		mvc.perform(delete("/api/events/1").with(user("Organiser").roles(Security.ORG_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("event 1"))).andExpect(jsonPath("$.id", equalTo(1)))
				.andExpect(handler().methodName("deleteEvent"));

		verify(eventService, never()).deleteById(1);
	}
	
	@Test
	public void deleteEventUnauth() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(delete("/api/events/1")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
		
		verify(eventService, never()).deleteById(1);
	}
	
	@Test
	public void postNewEvent() throws Exception {
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		Venue venue = new Venue();
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(eventService.save(any(Event.class))).then(returnsFirstArg());

		mvc.perform(post("/api/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Test event\", \"description\": \"Test description\", \"date\": \""+LocalDate.now().plusDays(1).toString()+"\", \"time\": \"12:00\", \"venueId\":\"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(content().string(""))
				.andExpect(header().string("Location", containsString("/api/events/")))
				.andExpect(handler().methodName("createEvent"));
		
		verify(eventService).save(arg.capture());
		assertThat("Test event", equalTo(arg.getValue().getName()));
		assertThat("Test description", equalTo(arg.getValue().getDescription()));
		assertThat(LocalDate.now().plusDays(1), equalTo(arg.getValue().getDate()));
		assertThat("12:00", equalTo(arg.getValue().getTime().toString()));
		assertThat(venue, equalTo(arg.getValue().getVenue()));
	}
	
	@Test
	public void postNewEventUnauth() throws Exception
	{
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		Venue venue = new Venue();
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(eventService.save(any(Event.class))).then(returnsFirstArg());
		
		mvc.perform(post("/api/events/new")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Test event\", \"description\": \"Test description\", \"date\": \"1970-01-01\", \"time\": \"12:00\", \"venue\":\"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
		
		verify(eventService, never()).save(arg.capture());
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

		mvc.perform(post("/api/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.param("name", "Test event").param("description", "Test description").param("date", LocalDate.now().plusDays(1).toString()).param("time", "12:00").param("venueId", "1").param("id", event.getId()+"")
				.content("{\"id\": \""+event.getId()+"\", \"name\": \"Test event\", \"description\": \"Test description\", \"date\": \""+LocalDate.now().plusDays(1).toString()+"\", \"time\": \"12:00\", \"venueId\":\"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(content().string(""))
				.andExpect(header().string("Location", containsString("/api/events/")))
				.andExpect(handler().methodName("updateEvent"));		
		
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
		
		mvc.perform(post("/api/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\", \"name\": \"Test event\", \"date\": \""+pastDate.toString()+"\", \"venueId\":\"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("updateEvent"));

		mvc.perform(post("/api/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Test event\", \"date\": \""+pastDate.toString()+"\", \"venueId\":\"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("createEvent"));

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
		
		mvc.perform(post("/api/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\", \"name\": \"Test event\", \"date\": \""+currentDate.toString()+"\", \"venueId\":\"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("updateEvent"));
				
		mvc.perform(post("/api/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Test event\", \"date\": \""+currentDate.toString()+"\", \"venueId\":\"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("createEvent"));;
		
		verify(eventService, never()).save(arg.capture());
	}
	
	@Test
	public void testOptionalDetails() throws Exception
	{	
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		Venue venue = new Venue();
		Event event = new Event();
		LocalDate futureDate = LocalDate.now().plusDays(1);
		
		event.setName("test");
		event.setVenue(venue);
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(eventService.findById(any(Long.class))).thenReturn(Optional.of(event));
		when(eventService.save(any(Event.class))).then(returnsFirstArg());
		
		mvc.perform(post("/api/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\", \"name\": \"Test event\", \"date\": \""+futureDate.toString()+"\", \"venueId\":\"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(content().string(""))
				.andExpect(header().string("Location", containsString("/api/events/")))
				.andExpect(handler().methodName("updateEvent"));
		
		mvc.perform(post("/api/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Test event\", \"date\": \""+futureDate.toString()+"\", \"venueId\":\"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(content().string(""))
				.andExpect(header().string("Location", containsString("/api/events/")))
				.andExpect(handler().methodName("createEvent"));
		
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
		
		mvc.perform(post("/api/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\", \"date\": \""+currentDate.toString()+"\", \"venueId\":\"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("updateEvent"));
		
		mvc.perform(post("/api/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"date\": \""+currentDate.toString()+"\", \"venueId\":\"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("createEvent"));
				
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
		
		mvc.perform(post("/api/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\", \"name\": \"Test event\", \"venueId\":\"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("updateEvent"));
		
		mvc.perform(post("/api/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Test event\", \"venueId\":\"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("createEvent"));
				
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
		
		mvc.perform(post("/api/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\", \"name\": \"Test event\", \"date\": \""+currentDate.toString()+"\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("updateEvent"));
		
		mvc.perform(post("/api/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Test event\", \"date\": \""+currentDate.toString()+"\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("createEvent"));
		
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
		
		mvc.perform(post("/api/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\", \"name\": \""+longName+"\", \"date\": \""+currentDate.toString()+"\", \"venueId\": \"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("updateEvent"));
		
		mvc.perform(post("/api/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \""+longName+"\", \"date\": \""+currentDate.toString()+"\", \"venueId\": \"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("createEvent"));
				
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
		
		mvc.perform(post("/api/events/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\", \"name\": \"Test event\", \"description\": \""+longDescr+"\", \"date\": \""+currentDate.toString()+"\", \"venueId\": \"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("updateEvent"));
		
		mvc.perform(post("/api/events/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Test event\", \"description\": \""+longDescr+"\", \"date\": \""+currentDate.toString()+"\", \"venueId\": \"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("createEvent"));
	}
		
	@Test
	void updateEventUnauth() throws Exception
	{
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		Venue venue = new Venue();
		Event event = new Event();
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(eventService.findById(any(Long.class))).thenReturn(Optional.of(event));
		when(eventService.save(any(Event.class))).then(returnsFirstArg());
		
		mvc.perform(post("/api/events/update")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\", \"name\": \"Test event\", \"description\": \"Test description\", \"date\": \"1970-01-01\", \"venue\":\"1\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
		
		verify(eventService, never()).save(arg.capture());
	}
}
