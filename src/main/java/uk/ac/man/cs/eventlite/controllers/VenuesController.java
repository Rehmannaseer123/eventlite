package uk.ac.man.cs.eventlite.controllers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.validation.Valid;

import org.hibernate.validator.internal.util.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import org.springframework.web.bind.annotation.DeleteMapping;

import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.config.data.InitialDataLoader;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;
import uk.ac.man.cs.eventlite.exceptions.VenueNotFoundException;

@Controller
@RequestMapping(value = "/venues", produces = { MediaType.TEXT_HTML_VALUE })
public class VenuesController {

	private final static Logger log = LoggerFactory.getLogger(VenuesController.class);

	@Autowired
	private VenueService venueService;
	
	@Autowired
	private EventService eventService;
	
	private static String MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoiYWxpaGFzYW4xIiwiYSI6ImNsZjZ1czc2MTB5cDAzeXFoZmdqaDFnYjAifQ.XWhZkhI4t1JLtsDc3sx0_A";

	@ExceptionHandler(VenueNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String venueNotFoundHandler(VenueNotFoundException ex, Model model) {
		model.addAttribute("not_found_id", ex.getId());
		model.addAttribute("heading", "All Venues");

		return "venues/not_found";
	}
	
	@GetMapping("/{id}")
	public String getVenue(@PathVariable("id") long id, Model model) {
		Venue venue = venueService.findById(id).orElseThrow(() -> new VenueNotFoundException(id));
		model.addAttribute("venue", venue);
		
		Iterable<Event> UpcomingEvents = eventService.findUpcomingEventsByVenue(venue);
		model.addAttribute("upcomingEvents", UpcomingEvents);
		
		Iterable<Event> pastEvents = eventService.findPastEventsByVenue(venue);
		model.addAttribute("pastEvents", pastEvents);

		
		return "venues/detail";
	}

	@GetMapping
	public String getAllVenues(Model model) {
		Iterable<Venue> allVenues = venueService.findAll();
        model.addAttribute("allVenues", allVenues);
		model.addAttribute("heading", "All Venues");

		return "venues/index";
	}
	

	@RequestMapping(value="/search", method=RequestMethod.GET)
	public String getVenuesByNameContaining(Model model, @RequestParam(value="nameSearch") String nameSearch) {
		
		model.addAttribute("allVenues", venueService.findAllByNameContainingIgnoreCase(nameSearch));
		model.addAttribute("heading", "Search Result");
		
		return "venues/index";
	}

	@GetMapping("/new")
	public String newVenues(Model model) {
		if (!model.containsAttribute("venue")) {
			model.addAttribute("venue", new Venue());
		}
		if (!model.containsAttribute("venue")) {
			model.addAttribute("venue", venueService.findAll());
		}

		return "venues/new";
	}
	
	@PostMapping(value = "/new", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String createVenue(@RequestBody @Valid @ModelAttribute Venue venue, BindingResult errors,
			Model model, RedirectAttributes redirectAttrs) {

		if (errors.hasErrors()) {
			model.addAttribute("venue", venue);
			return "venues/new";
		}
		
		Optional<Point> point = getAddressMapping(venue.getStreetName(), venue.getPostcode());
		
		if(point.isPresent()) {
			venue.setLatitude(point.get().latitude());
			venue.setLongitude(point.get().longitude());
		} else {
			model.addAttribute("venue", venue);
			errors.rejectValue("streetName", "error.user", "Failed to find address");
			errors.rejectValue("postcode", "error.user", "Failed to find address");
			return "venues/new";
		}
		
		redirectAttrs.addFlashAttribute("ok_message", "New venue added.");

		venueService.save(venue);
		

		return "redirect:/venues";
	}
	
	@GetMapping("/update")
	public String updateVenue(@RequestParam(value = "id") long id, Model model) {
		if (venueService.findById(id).isEmpty()) {
			model.addAttribute("not_found_id", id);
			return "venue/not_found";
		}
		if (!model.containsAttribute("venue")) {
			model.addAttribute("venue", venueService.findById(id).get());
		}

		return "venues/update";
	}
	
	@PostMapping(value = "/update", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String updateVenue(@RequestBody @Valid @ModelAttribute Venue venue, BindingResult errors,
			Model model, RedirectAttributes redirectAttrs) {
		
		
		if (errors.hasErrors()) {
			model.addAttribute("venue", venue);
			return "venues/update";
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
				model.addAttribute("venue", venue);
				errors.rejectValue("streetName", "error.user", "Failed to find address");
				errors.rejectValue("postcode", "error.user", "Failed to find address");
				return "venues/update";
			}
		} else {
			venue.setLatitude(prev.getLatitude());
			venue.setLongitude(prev.getLongitude());
		}
		
		venueService.save(venue);
		redirectAttrs.addFlashAttribute("ok_message", "Venue updated");

		return "redirect:/venues/"+venue.getId();
	}
	
	@DeleteMapping("/{id}")
	public String deleteVenue(@PathVariable("id") long id, RedirectAttributes redirectAttrs) {
		
		if (!venueService.existsById(id)) {
			throw new VenueNotFoundException(id);
		}
		try {
			String name = venueService.findById(id).get().getName();
			venueService.deleteById(id);
			redirectAttrs.addFlashAttribute("ok_message", "Venue" + name + "deleted.");
		} catch (Exception e) {
			String name = "successfully";
			venueService.deleteById(id);
			redirectAttrs.addFlashAttribute("ok_message", "Venue" + name + "deleted.");
		}

		return "redirect:/venues";
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
			Thread.sleep(1000);
		} catch (Exception e) {}
		
		return Optional.ofNullable(point[0]);
		
	}
}
