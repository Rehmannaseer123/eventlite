package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
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


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
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

@ExtendWith(SpringExtension.class)
@WebMvcTest(VenuesController.class)
@Import(Security.class)
public class VenuesControllerTest {

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
	public void getIndexWhenNoVenues() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>emptyList());

		mvc.perform(get("/venues").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("venues/index")).andExpect(handler().methodName("getAllVenues"));

		verify(venueService).findAll();
		verifyNoInteractions(venue);
	}

	@Test
	public void getIndexWithVenues() throws Exception {
		when(venue.getName()).thenReturn("Kilburn Building");
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));

		mvc.perform(get("/venues").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("venues/index")).andExpect(handler().methodName("getAllVenues"));

		verify(venueService).findAll();
	}

	@Test
	public void getVenueNotFound() throws Exception {
		mvc.perform(get("/venues/99").accept(MediaType.TEXT_HTML)).andExpect(status().isNotFound())
				.andExpect(view().name("venues/not_found")).andExpect(handler().methodName("getVenue"));
	}
	
	@Test
	public void searchVenueByNameWhenNameIsEmpty() throws Exception {

        when(venueService.findAllByNameContainingIgnoreCase("")).thenReturn(Collections.<Venue>emptyList());
		
		mvc.perform(get("/venues/search").accept(MediaType.TEXT_HTML).param("nameSearch", ""))
		.andExpect(status().isOk()).andExpect(view().name("venues/index"))
		.andExpect(model().attribute("allVenues", Collections.<Venue>emptyList()));
		
		verify(venueService).findAllByNameContainingIgnoreCase("");
	}
	
	@Test
	public void searchEventByNameWhenNameIsNotEmpty() throws Exception {
		
		String searchTerm = "Data";
		
		when(venueService.findAllByNameContainingIgnoreCase(searchTerm)).thenReturn(Collections.<Venue>emptyList());
		
		mvc.perform(get("/venues/search").accept(MediaType.TEXT_HTML).param("nameSearch", searchTerm))
		.andExpect(status().isOk()).andExpect(view().name("venues/index"))
		.andExpect(model().attribute("allVenues", Collections.<Venue>emptyList()));
		
		verify(venueService).findAllByNameContainingIgnoreCase(searchTerm);
	}
	
	@Test
	public void postNewVenue() throws Exception {
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());

		mvc.perform(post("/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn").param("streetName", "Oxford Road").param("postcode", "M13 9PL").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/venues")).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeExists("ok_message"));

		verify(venueService).save(arg.capture());
		assertThat("Kilburn", equalTo(arg.getValue().getName()));
		assertThat("Oxford Road", equalTo(arg.getValue().getStreetName()));
		assertThat("M13 9PL", equalTo(arg.getValue().getPostcode()));
		assertThat(100, equalTo(arg.getValue().getCapacity()));
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
		
		mvc.perform(post("/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn").param("streetName", "Oxford Road").param("postcode", "M13 9PL").param("capacity", "100").param("id", venue.getId()+"")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/venues/"+venue.getId())).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeExists("ok_message"));

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
		
		mvc.perform(post("/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("streetName", "Oxford Road").param("postcode", "M13 9PL").param("capacity", "100").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "name"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		mvc.perform(post("/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("streetName", "Oxford Road").param("postcode", "M13 9PL").param("capacity", "100").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "name"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(arg.capture());
	}
	
	@Test
	public void testMissingStreetName() throws Exception
	{	
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		Venue venue = new Venue();
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn").param("postcode", "M13 9PL").param("capacity", "100").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "streetName"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		mvc.perform(post("/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn").param("postcode", "M13 9PL").param("capacity", "100").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "streetName"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(arg.capture());
	}
	
	@Test
	public void testMissingPostcode() throws Exception
	{	
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		Venue venue = new Venue();
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("streetName", "Oxford Road").param("name", "Kilburn").param("capacity", "100").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "postcode"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		mvc.perform(post("/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("streetName", "Oxford Road").param("name", "Kilburn").param("capacity", "100").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "postcode"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(arg.capture());
	}
	
	@Test
	public void testMissingcapacity() throws Exception
	{	
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		Venue venue = new Venue();
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("streetName", "Oxford Road").param("postcode", "M13 9PL").param("name", "Kilburn").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "capacity"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		mvc.perform(post("/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("streetName", "Oxford Road").param("postcode", "M13 9PL").param("name", "Kilburn").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "capacity"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));
		
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
		
		mvc.perform(post("/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", longName).param("streetName", "Oxford Road").param("postcode", "M13 9PL").param("capacity", "100").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "name"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		mvc.perform(post("/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", longName).param("streetName", "Oxford Road").param("postcode", "M13 9PL").param("capacity", "100").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "name"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));
		
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
		
		mvc.perform(post("/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn").param("streetName", longStreetName).param("postcode", "M13 9PL").param("capacity", "100").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "streetName"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		mvc.perform(post("/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn").param("streetName", longStreetName).param("postcode", "M13 9PL").param("capacity", "100").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "streetName"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(arg.capture());
	}
	
	@Test
	public void testcapacityNegative() throws Exception {	
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		Venue venue = new Venue();
		int capacity = -1;

		venue.setCapacity(capacity);
		
		when(venueService.findById(any(Long.class))).thenReturn(Optional.of(venue));
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn").param("streetName", "Oxford Road").param("postcode", "M13 9PL").param("capacity", capacity + "").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "capacity"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		mvc.perform(post("/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn").param("streetName", "Oxford Road").param("postcode", "M13 9PL").param("capacity", capacity + "").param("id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "capacity"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(arg.capture());
	}

	@Test
	public void getVenueWithoutEvents() throws Exception {
		when(venueService.findById(1)).thenReturn(Optional.of(venue));
		when(event.getVenue()).thenReturn(new Venue());
		when(eventService.findUpcomingEventsByVenue(venue)).thenReturn(Collections.<Event>emptyList());

		mvc.perform(get("/venues/1").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("venues/detail")).andExpect(handler().methodName("getVenue"));

		verify(eventService).findUpcomingEventsByVenue(venue);
		verify(venueService).findById(1);
		verifyNoInteractions(event);
	}
	
	@Test
	public void getVenueWithEvents() throws Exception {
		when(venueService.findById(1)).thenReturn(Optional.of(venue));
		when(event.getVenue()).thenReturn(new Venue());
		when(eventService.findUpcomingEventsByVenue(venue)).thenReturn(Collections.<Event>singletonList(event));

		mvc.perform(get("/venues/1").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("venues/detail")).andExpect(handler().methodName("getVenue"));

		verify(eventService).findUpcomingEventsByVenue(venue);
		verify(venueService).findById(1);
	}
	
	
	@Test
	public void getVenueName() throws Exception {
		when(event.getVenue()).thenReturn(new Venue());
		when(venueService.findById(1)).thenReturn(Optional.of(venue));
		when(eventService.findUpcomingEventsByVenue(venue)).thenReturn(Collections.<Event>emptyList());


		mvc.perform(get("/venues/1?name=Kilburn").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("venues/detail")).andExpect(handler().methodName("getVenue"));
		
		verify(eventService).findUpcomingEventsByVenue(venue);
		verify(venueService).findById(1);
		verifyNoInteractions(event);
	}
	
	@Test
	public void deleteVenue() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(delete("/venues/1").with(user("Organiser").roles(Security.ORG_ROLE)).accept(MediaType.TEXT_HTML).with(csrf()))
		.andExpect(status().isFound()).andExpect(view().name("redirect:/venues"))
				.andExpect(handler().methodName("deleteVenue")).andExpect(flash().attributeExists("ok_message"));

	}
	
	@Test
	public void deleteVenueNotFound() throws Exception {
		when(venueService.existsById(1)).thenReturn(false);

		mvc.perform(delete("/venues/1").with(user("Organiser").roles(Security.ORG_ROLE)).accept(MediaType.TEXT_HTML).with(csrf()))
		.andExpect(status().isNotFound()).andExpect(view().name("venues/not_found"))
				.andExpect(handler().methodName("deleteVenue"));

		verify(venueService, never()).deleteById(1);
	}
	
	@Test
	public void testGeocodingNew() throws Exception {
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn").param("streetName", "Oxford Road").param("postcode", "M13 9PL").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/venues")).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeExists("ok_message"));

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
		
		mvc.perform(post("/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn").param("streetName", "Oxford Road").param("postcode", "M13 9PL").param("capacity", "100").param("id", venue.getId()+"")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/venues/"+venue.getId())).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeExists("ok_message"));

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
		
		mvc.perform(post("/venues/update").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn").param("streetName", "Garbage Address").param("postcode", "A99 9AA").param("capacity", "100").param("id", venue.getId()+"")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "streetName"))
				.andExpect(model().attributeHasFieldErrors("venue", "postcode"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		mvc.perform(post("/venues/new").with(user("Organiser").roles(Security.ORG_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn").param("streetName", "Garbage Address").param("postcode", "A99 9AA").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "streetName"))
				.andExpect(model().attributeHasFieldErrors("venue", "postcode"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(arg.capture());
	}
}
