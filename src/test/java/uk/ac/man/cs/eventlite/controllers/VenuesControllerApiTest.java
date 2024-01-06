package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import uk.ac.man.cs.eventlite.assemblers.VenueModelAssembler;
import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
@ExtendWith(SpringExtension.class)
@WebMvcTest(VenuesControllerApi.class)
@Import({ Security.class, VenueModelAssembler.class, EventModelAssembler.class })
public class VenuesControllerApiTest {

	@Autowired
	private MockMvc mvc;

	@MockBean
	private VenueService venueService;
	
	@MockBean
	private EventService eventService;

	@Test
	public void getIndexWhenNoVenues() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>emptyList());

		mvc.perform(get("/api/venues").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllVenues")).andExpect(jsonPath("$.length()", equalTo(1)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues")));

		verify(venueService).findAll();
	}

	@Test
	public void getIndexWithVenues() throws Exception {
		Venue v = new Venue();
		v.setId(0);
		v.setName("Venue");
		
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(v));

		mvc.perform(get("/api/venues").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllVenues")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues")))
				.andExpect(jsonPath("$._embedded.venues.length()", equalTo(1)));

		verify(venueService).findAll();
	}
	
	@Test
	public void getVenueEvents() throws Exception {
		Venue v = new Venue();
		v.setId(0);
		v.setName("Venue");
		Event e1 = new Event();
		e1.setName("event1");
		e1.setVenue(v);
		v.getEvents().add(e1);
		Event e2 = new Event();
		e2.setName("event2");
		e2.setVenue(v);
		v.getEvents().add(e2);
		
		when(venueService.findById(0)).thenReturn(Optional.of(v));

		mvc.perform(get("/api/venues/0/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getVenueEvents")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/0/events")))
				.andExpect(jsonPath("$._embedded.events.length()", equalTo(2)));

		verify(venueService).findById(0);
	}
	
	@Test
	public void getVenueNextEventsLessThan3() throws Exception {
		Venue v = new Venue();
		v.setId(0);
		v.setName("Venue");
		Event e1 = new Event();
		e1.setName("event1");
		e1.setVenue(v);
		e1.setDate(LocalDate.of(1970, 1, 1));
		v.getEvents().add(e1);
		Event e2 = new Event();
		e2.setName("event2");
		e2.setVenue(v);
		e2.setDate(LocalDate.of(1970, 1, 1));
		v.getEvents().add(e2);		
		
		when(venueService.findById(0)).thenReturn(Optional.of(v));
		when(eventService.findUpcomingEventsByVenue(v)).thenReturn(v.getEvents());

		mvc.perform(get("/api/venues/0/next3events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getVenueNextEvents")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/0/next3events")))
				.andExpect(jsonPath("$._embedded.events.length()", equalTo(2)));

		verify(venueService).findById(0);
	}
	
	@Test
	public void getVenueNextEventsMoreThan3() throws Exception {
		Venue v = new Venue();
		v.setId(0);
		v.setName("Venue");
		Event e1 = new Event();
		e1.setName("event1");
		e1.setVenue(v);
		e1.setDate(LocalDate.of(1970, 1, 1));
		v.getEvents().add(e1);
		Event e2 = new Event();
		e2.setName("event2");
		e2.setVenue(v);
		e2.setDate(LocalDate.of(1970, 1, 1));
		e2.setTime(LocalTime.of(1, 0));
		v.getEvents().add(e2);
		Event e3 = new Event();
		e3.setName("event3");
		e3.setVenue(v);
		e3.setDate(LocalDate.of(1970, 1, 2));
		e3.setTime(LocalTime.of(1, 0));
		v.getEvents().add(e3);
		Event e4 = new Event();
		e4.setName("event4");
		e4.setVenue(v);
		e4.setDate(LocalDate.of(1970, 1, 2));
		e4.setTime(LocalTime.of(2, 0));
		v.getEvents().add(e4);
		
		
		when(venueService.findById(0)).thenReturn(Optional.of(v));
		when(eventService.findUpcomingEventsByVenue(v)).thenReturn(v.getEvents());

		mvc.perform(get("/api/venues/0/next3events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getVenueNextEvents")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/0/next3events")))
				.andExpect(jsonPath("$._embedded.events.length()", equalTo(3)))
				.andExpect(jsonPath("$._embedded.events[0].name", equalTo("event1")))
				.andExpect(jsonPath("$._embedded.events[1].name", equalTo("event2")))
				.andExpect(jsonPath("$._embedded.events[2].name", equalTo("event3")));

		verify(venueService).findById(0);
	}

	@Test
	public void getVenueNotFound() throws Exception {
		mvc.perform(get("/api/venues/99").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("venue 99"))).andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("getVenue"));
	}
	
	@Test
	public void searchVenuesByNameWhenNameIsNotEmpty() throws Exception {
		Venue testVenue1 = new Venue();
		testVenue1.setCapacity(100);
		testVenue1.setName("Kilburn");
		venueService.save(testVenue1);
		Venue testVenue2 = new Venue();
		testVenue2.setCapacity(100);
		testVenue2.setName("Soft Engineering Building");
		venueService.save(testVenue2);
		when(venueService.findAllByNameContainingIgnoreCase("Kil")).thenReturn(Collections.<Venue>singletonList(testVenue1));

		mvc.perform(get("/api/venues/search").accept(MediaType.APPLICATION_JSON).param("nameSearch", "Kil")).andExpect(status().isOk())
				.andExpect(handler().methodName("getVenuesByNameContaining")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/search?nameSearch=Kil")))
				.andExpect(jsonPath("$._embedded.venues.length()", equalTo(1)));

		verify(venueService).findAllByNameContainingIgnoreCase("Kil");
	}
		
	@Test
	public void postNewVenue() throws Exception {
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());

		mvc.perform(post("/api/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Kilburn\", \"streetName\": \"Oxford Road\", \"postcode\": \"M13 9PL\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(content().string(""))
				.andExpect(header().string("Location", containsString("/api/venues/")))
				.andExpect(handler().methodName("createVenue"));
		
		verify(venueService).save(arg.capture());
		assertThat("Kilburn", equalTo(arg.getValue().getName()));
		assertThat("Oxford Road", equalTo(arg.getValue().getStreetName()));
		assertThat("M13 9PL", equalTo(arg.getValue().getPostcode()));
		assertThat(100, equalTo(arg.getValue().getCapacity()));
	}
	
	@Test
	public void postNewVenueUnauth() throws Exception
	{
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/api/venues/new")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Kilburn\", \"streetName\": \"Oxford Road\", \"postcode\": \"M13 9PL\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
		
		verify(venueService, never()).save(arg.capture());
	}
	
	@Test
	public void updateVenue() throws Exception {
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		Venue venue = new Venue();
		venue.setId(1);
		venue.setName("Kilburn");
		venue.setStreetName("Oxford Road");
		venue.setPostcode("M13 9PL");
		venue.setCapacity(100);
		
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));

		mvc.perform(post("/api/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \""+venue.getId()+"\" , \"name\": \"Kilburn\", \"streetName\": \"Oxford Road\", \"postcode\": \"M13 9PL\", \"capacity\": \"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(content().string(""))
				.andExpect(header().string("Location", containsString("/api/venues/")))
				.andExpect(handler().methodName("updateVenue"));		
		
		verify(venueService).save(arg.capture());
		assertThat("Kilburn", equalTo(arg.getValue().getName()));
		assertThat("Oxford Road", equalTo(arg.getValue().getStreetName()));
		assertThat("M13 9PL", equalTo(arg.getValue().getPostcode()));
		assertThat(100, equalTo(arg.getValue().getCapacity()));
	}
	
	@Test
	public void testMissingName() throws Exception
	{	
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		Venue venue = new Venue();
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/api/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\" , \"streetName\": \"Oxford Road\", \"postcode\": \"M13 9PL\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("updateVenue"));
		
		mvc.perform(post("/api/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"streetName\": \"Oxford Road\", \"postcode\": \"M13 9PL\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("createVenue"));
				
		verify(venueService, never()).save(arg.capture());
	}
	
	@Test
	public void testMissingStreetName() throws Exception
	{	
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		Venue venue = new Venue();
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/api/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\" , \"name\": \"Kilburn\", \"postcode\": \"M13 9PL\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("updateVenue"));
		
		mvc.perform(post("/api/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Kilburn\", \"postcode\": \"M13 9PL\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("createVenue"));
				
		verify(venueService, never()).save(arg.capture());
	}
	
	@Test
	public void testMissingPostcode() throws Exception
	{	
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		Venue venue = new Venue();
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/api/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\" , \"streetName\": \"Oxford Road\", \"name\": \"Kilburn\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("updateVenue"));
		
		mvc.perform(post("/api/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"streetName\": \"Oxford Road\", \"name\": \"Kilburn\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("createVenue"));
				
		verify(venueService, never()).save(arg.capture());
	}
	
	@Test
	public void testMissingcapacity() throws Exception
	{	
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		Venue venue = new Venue();
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/api/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\" , \"streetName\": \"Oxford Road\", \"postcode\": \"M13 9PL\", \"name\":\"Kilburn\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("updateVenue"));
		
		mvc.perform(post("/api/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"streetName\": \"Oxford Road\", \"postcode\": \"M13 9PL\", \"name\":\"Kilburn\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("createVenue"));
				
		verify(venueService, never()).save(arg.capture());
	}
	

	@Test
	public void testNameTooLong() throws Exception
	{	
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		Venue venue = new Venue();
		String longName = new String(new char[257]).replace("\0", "1");

		venue.setName(longName);
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/api/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\" , \"name\": \""+longName+"\", \"streetName\": \"Oxford Road\", \"postcode\": \"M13 9PL\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("updateVenue"));
		
		mvc.perform(post("/api/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \""+longName+"\", \"streetName\": \"Oxford Road\", \"postcode\": \"M13 9PL\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("createVenue"));
				
		verify(venueService, never()).save(arg.capture());
	}
	
	@Test
	public void testStreetNameTooLong() throws Exception
	{	
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		Venue venue = new Venue();
		String longStreetName = new String(new char[301]).replace("\0", "1");

		venue.setStreetName(longStreetName);
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/api/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\" , \"name\": \"Kilburn\", \"streetName\": \""+longStreetName+"\", \"postcode\": \"M13 9PL\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("updateVenue"));
		
		mvc.perform(post("/api/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Kilburn\", \"streetName\": \""+longStreetName+"\", \"postcode\": \"M13 9PL\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("createVenue"));
				
		verify(venueService, never()).save(arg.capture());
	}
	
	@Test
	public void testcapacityNegative() throws Exception
	{	
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		Venue venue = new Venue();
		int capacity = -1;

		venue.setCapacity(capacity);
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/api/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\" , \"name\": \"Kilburn\", \"streetName\": \"Oxford Road\", \"postcode\": \"M13 9PL\", \"capacity\":\""+capacity+"\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("updateVenue"));
		
		mvc.perform(post("/api/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Kilburn\", \"streetName\": \"Oxford Road\", \"postcode\": \"M13 9PL\", \"capacity\":\""+capacity+"\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("createVenue"));
				
		verify(venueService, never()).save(arg.capture());
	}
	
	@Test
	public void updateVenueUnauth() throws Exception {
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		Venue venue = new Venue();
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/api/venues/update")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\" , \"name\": \"Kilburn\", \"streetName\": \"Oxford Road\", \"postcode\": \"M13 9PL\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
		
		verify(venueService, never()).save(arg.capture());
	}

	@Test
	public void getVenue() throws Exception {
		Venue v = new Venue();
		v.setId(1);
		v.setName("Kilburn");
				
		when(venueService.findById(1)).thenReturn(Optional.of(v));
		
		mvc.perform(get("/api/venues/1").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getVenue"))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/1")))
				.andExpect(jsonPath("$._links.venue.href", endsWith("/api/venues/1")))
				.andExpect(jsonPath("$._links.events.href", endsWith("/api/venues/1/events")))
				.andExpect(jsonPath("$._links.next3events.href", endsWith("/api/venues/1/next3events")))
				.andExpect(jsonPath("$.id", equalTo(1)));
		
		verify(venueService).findById(1);
	}
	
	@Test
	public void deleteVenue() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		when(venueService.findById(1)).thenReturn(Optional.of(new Venue()));
		mvc.perform(delete("/api/venues/1").with(user("Organiser").roles(Security.ORG_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent())
				.andExpect(content().string(""))
				.andExpect(handler().methodName("deleteVenue"));

		verify(venueService).deleteById(1);
	}
	
	@Test
	public void deleteVenueNotFound() throws Exception {
		when(venueService.existsById(1)).thenReturn(false);

		mvc.perform(delete("/api/venues/1").with(user("Organiser").roles(Security.ORG_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("venue 1"))).andExpect(jsonPath("$.id", equalTo(1)))
				.andExpect(handler().methodName("deleteVenue"));

		verify(venueService, never()).deleteById(1);
	}
	
	@Test
	public void deleteVenueUnauth() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(delete("/api/venues/1")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
		
		verify(venueService, never()).deleteById(1);

	}
	
	@Test
	public void testGeocodingNew() throws Exception {
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/api/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Kilburn\", \"streetName\": \"Oxford Road\", \"postcode\": \"M13 9PL\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(content().string(""))
				.andExpect(header().string("Location", containsString("/api/venues/")))
				.andExpect(handler().methodName("createVenue"));

		verify(venueService).save(arg.capture());
		assertTrue(arg.getValue().getLatitude() >= 53.46 && arg.getValue().getLatitude() <= 53.47);
		assertTrue(arg.getValue().getLongitude() >= -2.24 && arg.getValue().getLongitude() <= -2.23);
	}
	
	@Test
	public void testGeocodingUpdate() throws Exception {
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		Venue venue = new Venue();
		venue.setId(1);
		venue.setName("Kilburn");
		venue.setStreetName("Kilburn Building");
		venue.setPostcode("M13 9PL");
		venue.setCapacity(100);
		
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		
		mvc.perform(post("/api/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \""+venue.getId()+"\" , \"name\": \"Kilburn\", \"streetName\": \"Oxford Road\", \"postcode\": \"M13 9PL\", \"capacity\": \"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(content().string(""))
				.andExpect(header().string("Location", containsString("/api/venues/")))
				.andExpect(handler().methodName("updateVenue"));		
		
		verify(venueService).save(arg.capture());
		assertTrue(arg.getValue().getLatitude() >= 53.46 && arg.getValue().getLatitude() <= 53.47);
		assertTrue(arg.getValue().getLongitude() >= -2.24 && arg.getValue().getLongitude() <= -2.23);
	}
	
	@Test
	public void testGeocodingGarbageAddress() throws Exception {
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		Venue venue = new Venue();
		venue.setStreetName("Oxford Road");
		venue.setPostcode("M13 9PL");
		
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		
		mvc.perform(post("/api/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\": \"1\" , \"name\": \"Kilburn\", \"streetName\": \"Garbage Address\", \"postcode\": \"A99 9AA\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("updateVenue"));
		
		mvc.perform(post("/api/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Kilburn\", \"streetName\": \"Garbage Address\", \"postcode\": \"A99 9AA\", \"capacity\":\"100\"}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(header().string("Location", equalTo(null))).andExpect(handler().methodName("createVenue"));
		
		verify(venueService, never()).save(arg.capture());
	}
}
