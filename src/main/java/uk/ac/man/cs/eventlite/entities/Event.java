package uk.ac.man.cs.eventlite.entities;

import java.time.LocalDate;
import javax.persistence.*;
import javax.validation.constraints.Future;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity
@Table(name="Event")
public class Event {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(name = "Date")

	@NotNull(message = "Event must have a date")
	@Future(message = "Date must be in the future")
	@JsonFormat(shape = JsonFormat.Shape.STRING)
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate date;

	@Column(name = "Time")
	@JsonFormat(shape = JsonFormat.Shape.STRING)
	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime time;

	@Column(name = "Name")
	@NotEmpty(message = "Event must have a name")
	@Size(max=255, message="Name must be < 256 characters")
	private String name;
	
	@Column(name = "Description", length=500)
	@Size(max=499, message="Description must be < 500 characters")
	private String description;

	@ManyToOne
	@JoinColumn(name="venue_id")
	@JsonIgnore
	private Venue venue;
	
	@Transient
	@Min(value=1, message = "Event must have a venue")
	private long venueId;

	public Event() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public LocalTime getTime() {
		return time;
	}

	public void setTime(LocalTime time) {
		this.time = time;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Venue getVenue() {
		return venue;
	}

	public void setVenue(Venue venue) {
		this.venue = venue;
	}
	
	public long getVenueId() {
		return venueId;
	}

	public void setVenueId(long id) {
		this.venueId = id;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
}
