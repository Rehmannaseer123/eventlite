package uk.ac.man.cs.eventlite.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import org.springframework.web.bind.annotation.DeleteMapping;

import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;
import uk.ac.man.cs.eventlite.assemblers.VenueModelAssembler;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.VenueNotFoundException;


@RestController
@RequestMapping(value = "/api/venues", produces = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE })
public class VenuesControllerApi {

	private final static Logger log = LoggerFactory.getLogger(VenuesController.class);
	
	private static final String NOT_FOUND_MSG = "{ \"error\": \"%s\", \"id\": %d }";

	@Autowired
	private VenueService venueService;

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueModelAssembler venueAssembler;
	
	@Autowired
	private EventModelAssembler eventAssembler;

	private static String MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoiYWxpaGFzYW4xIiwiYSI6ImNsZjZ1czc2MTB5cDAzeXFoZmdqaDFnYjAifQ.XWhZkhI4t1JLtsDc3sx0_A";

	@ExceptionHandler(VenueNotFoundException.class)
	public ResponseEntity<?> venueNotFoundHandler(VenueNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(String.format(NOT_FOUND_MSG, ex.getMessage(), ex.getId()));
	}

	@RequestMapping(value="/{id}", method=RequestMethod.GET)
	public EntityModel<Venue> getVenue(@PathVariable("id") long id) {
		Venue venue = venueService.findById(id).orElseThrow(() -> new VenueNotFoundException(id));
		
		return venueAssembler.toModel(venue);
	}
	
	@RequestMapping(value="/{id}/events", method=RequestMethod.GET)
	public CollectionModel<EntityModel<Event>> getVenueEvents(@PathVariable("id") long id) {
		Venue venue = venueService.findById(id).orElseThrow(() -> new VenueNotFoundException(id));
		
		return eventAssembler.toCollectionModel(venue.getEvents())
				.add(linkTo(methodOn(VenuesControllerApi.class).getVenueEvents(id)).withSelfRel());
	}
	
	@RequestMapping(value="/{id}/next3events", method=RequestMethod.GET)
	public CollectionModel<EntityModel<Event>> getVenueNextEvents(@PathVariable("id") long id) {
		Venue venue = venueService.findById(id).orElseThrow(() -> new VenueNotFoundException(id));
		
		Iterable<Event> upcomingEvents = eventService.findUpcomingEventsByVenue(venue);
		
		ArrayList<Event> next3Events = new ArrayList<Event>();
		for(Event event: upcomingEvents) {
			next3Events.add(event);
		}
		
		next3Events.sort((e1, e2) -> {
			if (!e1.getDate().equals(e2.getDate())) {
				return e1.getDate().compareTo(e2.getDate());
			}
			if (e1.getTime() == null) {
				return -1;
			} else if (e2.getTime() == null) {
				return 1;
			}
			
			return e1.getTime().compareTo(e2.getTime());
		});
		if (next3Events.size() >= 3) {
			next3Events = new ArrayList<Event>(next3Events.subList(0, 3));
		}
		
		return eventAssembler.toCollectionModel(next3Events)
				.add(linkTo(methodOn(VenuesControllerApi.class).getVenueNextEvents(id)).withSelfRel());
	}

	@GetMapping
	public CollectionModel<EntityModel<Venue>> getAllVenues() {
		return venueAssembler.toCollectionModel(venueService.findAll())
				.add(linkTo(methodOn(VenuesControllerApi.class).getAllVenues()).withSelfRel());
	}

	@RequestMapping(value="/search", method=RequestMethod.GET)
	public CollectionModel<EntityModel<Venue>> getVenuesByNameContaining(@RequestParam(value="nameSearch") String nameSearch) {
		
		return venueAssembler.toCollectionModel(venueService.findAllByNameContainingIgnoreCase(nameSearch))
				.add(linkTo(methodOn(VenuesControllerApi.class).getVenuesByNameContaining(nameSearch)).withSelfRel());
	}
	
	@GetMapping("/new")
	public ResponseEntity<?> newVenue() {
		return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
	}

	@PostMapping(value = "/new", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> createVenue(@RequestBody @Valid Venue venue, BindingResult result) {

		if (result.hasErrors()) {
			return ResponseEntity.unprocessableEntity().build();
		}
		
		Optional<Point> point = getAddressMapping(venue.getStreetName(), venue.getPostcode());
		
		if(point.isPresent()) {
			venue.setLatitude(point.get().latitude());
			venue.setLongitude(point.get().longitude());
		} else {
			result.rejectValue("streetName", "error.user", "Failed to find address");
			result.rejectValue("postcode", "error.user", "Failed to find address");
			return ResponseEntity.unprocessableEntity().build();
		}
		
		Venue newVenue = venueService.save(venue);
		EntityModel<Venue> entity = venueAssembler.toModel(newVenue);

		return ResponseEntity.created(entity.getRequiredLink(IanaLinkRelations.SELF).toUri()).build();
	}
	
	@PostMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateVenue(@RequestBody @Valid Venue venue, BindingResult result) {

		if (result.hasErrors()) {
			return ResponseEntity.unprocessableEntity().build();
		}
		
		Venue prev = venueService.findById(venue.getId()).get();
		// only get new geocoding data if the street name or postcode have changed
		if (!prev.getStreetName().equals(venue.getStreetName())
				|| !prev.getPostcode().equals(venue.getPostcode())) {
			
			Optional<Point> point = getAddressMapping(venue.getStreetName(), venue.getPostcode());
			
			if(point.isPresent()) {
				venue.setLatitude(point.get().latitude());
				venue.setLongitude(point.get().longitude());
			} else {
				result.rejectValue("streetName", "error.user", "Failed to find address");
				result.rejectValue("postcode", "error.user", "Failed to find address");
				return ResponseEntity.unprocessableEntity().build();
			}
		}
		
		Venue newVenue = venueService.save(venue);
		EntityModel<Venue> entity = venueAssembler.toModel(newVenue);

		return ResponseEntity.created(entity.getRequiredLink(IanaLinkRelations.SELF).toUri()).build();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteVenue(@PathVariable("id") long id) {
		
		if (!venueService.existsById(id)) {
			throw new VenueNotFoundException(id);
		}
		
		if(venueService.findById(id).get().getEvents().size() >= 1) {
			return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
		}

		venueService.deleteById(id);

		return ResponseEntity.noContent().build();
	}
	
	private Optional<Point> getAddressMapping(String streetAddress, String postCode) {
		final double RELEVANCE = 0.49;
		final Point[] point = new Point[1];
		MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder()
				.accessToken(MAPBOX_ACCESS_TOKEN)
				.query(streetAddress + " " + postCode)
				.build();
		mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
			@Override
			public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
				List<CarmenFeature> results = response.body().features();
				if (results.size() > 0) {
					if (results.get(0).relevance() > RELEVANCE) {
						Point firstResultPoint = results.get(0).center();
					  	point[0] = firstResultPoint;
					  	log.info(firstResultPoint.toString());
					}
				}
			}
			@Override
			public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
				throwable.printStackTrace();
			}
		});
		
		try {
			Thread.sleep(1500);
		} catch (Exception e) {}
		
		return Optional.ofNullable(point[0]);
		
	}
}
