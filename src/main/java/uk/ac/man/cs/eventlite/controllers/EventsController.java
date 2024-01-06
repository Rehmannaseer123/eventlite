package uk.ac.man.cs.eventlite.controllers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.validation.Valid;

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
import org.springframework.web.bind.annotation.DeleteMapping;

import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;
import uk.ac.man.cs.eventlite.mastodon.MastodonService;
import uk.ac.man.cs.eventlite.mastodon.Post;

@Controller
@RequestMapping(value = "/events", produces = { MediaType.TEXT_HTML_VALUE })
public class EventsController {

	private final static Logger log = LoggerFactory.getLogger(EventsController.class);

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;
	
	private MastodonService mastodon = MastodonService.getInstance();

	@ExceptionHandler(EventNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String eventNotFoundHandler(EventNotFoundException ex, Model model) {
		model.addAttribute("not_found_id", ex.getId());
		model.addAttribute("heading", "All Events");

		return "events/not_found";
	}

	@RequestMapping(value="/{id}", method=RequestMethod.GET)
	public String getEvent(@PathVariable("id") long id, Model model) {
		Event event = eventService.findById(id).orElseThrow(() -> new EventNotFoundException(id));
		model.addAttribute("events", event);
		
		return "events/detail";
	}
	
	@PostMapping(value="/{id}")
	public String getEventSendPost(@PathVariable("id") long id, @RequestParam(value = "postContent") String content, Model model) {
		Event event = eventService.findById(id).orElseThrow(() -> new EventNotFoundException(id));
		model.addAttribute("events", event);
		
		if (content != null && !content.trim().isEmpty()){
				mastodon.publishStatus(content);
				model.addAttribute("content", content);
		}
		
		return "events/detail";
	}

	@GetMapping
	public String getAllEvents(Model model) {		
		Iterable<Event> pastEvents = eventService.findPastEvents();
        Iterable<Event> upcomingEvents = eventService.findUpcomingEvents();
        Iterable<Venue> venues = StreamSupport.stream(upcomingEvents.spliterator(), false).map(Event::getVenue).collect(Collectors.toList());
        model.addAttribute("pastEvents", pastEvents);
        model.addAttribute("upcomingEvents", upcomingEvents);
        model.addAttribute("upcomingEventsVenues", venues);
       
        Iterable<Post> posts = mastodon.readHome();
        model.addAttribute("posts", posts);
        
		model.addAttribute("heading", "All Events");

		return "events/index";
	}
	
	@RequestMapping(value="/search", method=RequestMethod.GET)
	public String getEventsByNameContaining(Model model, @RequestParam(value="nameSearch") String nameSearch) {
		Iterable<Venue> venues = StreamSupport.stream(eventService.findUpcomingEventsByNameContainingIgnoreCase(nameSearch).spliterator(), false).map(Event::getVenue).collect(Collectors.toList());
		model.addAttribute("pastEvents", eventService.findPastEventsByNameContainingIgnoreCase(nameSearch));
        model.addAttribute("upcomingEvents", eventService.findUpcomingEventsByNameContainingIgnoreCase(nameSearch));
        model.addAttribute("upcomingEventsVenues", venues);
		model.addAttribute("heading", "Search Result");
		
		return "events/search";
	}
	
	@DeleteMapping("/{id}")
	public String deleteEvent(@PathVariable("id") long id, RedirectAttributes redirectAttrs) {
		
		if (!eventService.existsById(id)) {
			throw new EventNotFoundException(id);
		}
		try {
			String name = eventService.findById(id).get().getName();
			eventService.deleteById(id);
			redirectAttrs.addFlashAttribute("ok_message", "Event" + name + "deleted.");
		} catch (Exception e) {
			String name = "successfully";
			eventService.deleteById(id);
			redirectAttrs.addFlashAttribute("ok_message", "Event" + name + "deleted.");
		}

		return "redirect:/events";
	}

	
	@GetMapping("/new")
	public String newEvent(Model model) {
		if (!model.containsAttribute("event")) {
			model.addAttribute("event", new Event());
		}
		if (!model.containsAttribute("venues")) {
			model.addAttribute("venues", venueService.findAll());
		}

		return "events/new";
	}
	
	@PostMapping(value = "/new", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String createEvent(@RequestBody @Valid @ModelAttribute Event event, BindingResult errors,
			Model model, RedirectAttributes redirectAttrs) {

		if (errors.hasErrors()) {
			model.addAttribute("venues", venueService.findAll());
			model.addAttribute("event", event);
			return "events/new";
		}
		
		redirectAttrs.addFlashAttribute("ok_message", "New event added.");

		Optional<Venue> venue = venueService.findById(event.getVenueId());
		if (venue.isEmpty()) {
			return "redirect:/events/new";
		}
		event.setVenue(venue.get());
		eventService.save(event);
		

		return "redirect:/events";
	}
	
	@GetMapping("/update")
	public String updateEvent(@RequestParam(value = "id") long id, Model model) {
		if (eventService.findById(id).isEmpty()) {
			model.addAttribute("not_found_id", id);
			return "events/not_found";
		}
		if (!model.containsAttribute("event")) {
			model.addAttribute("event", eventService.findById(id).get());
		}
		if (!model.containsAttribute("venues")) {
			model.addAttribute("venues", venueService.findAll());
		}

		return "events/update";
	}
	
	@PostMapping(value = "/update", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String updateEvent(@RequestBody @Valid @ModelAttribute Event event, BindingResult errors,
			Model model, RedirectAttributes redirectAttrs) {
		
		Optional<Venue> venue = venueService.findById(event.getVenueId());
		if (venue.isEmpty()) {
			return "redirect:/events";
		}
		event.setVenue(venue.get());
		
		if (errors.hasErrors()) {
			model.addAttribute("event", event);
			return "events/update";
		}
		
		
		eventService.save(event);
		redirectAttrs.addFlashAttribute("ok_message", "Event updated");

		return "redirect:/events/"+event.getId();
	}

}
