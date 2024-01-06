package uk.ac.man.cs.eventlite.entities;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.mapbox.geojson.Point;

@Entity
@Table(name = "venues")

public class Venue {
	
	@Id
	@GeneratedValue
	private long id;

	@NotEmpty(message = "Venue must have a name")
	@Size(max=255, message="Name must be < 256 characters")
	private String name;

	@NotEmpty(message = "Venue must have a street name")
	@Size(max=299, message="Street  must be < 300 characters")
	private String streetName;
	
	@NotEmpty(message = "Venue must have a postcode")
	@Pattern(regexp="^[A-Za-z]{1,2}[0-9][0-9A-Za-z]?\\s[0-9][A-Za-z]{2}$", message = "Postcode must be valid")
	private String postcode;
	
	private double latitude;
	
	private double longitude;


	@NotNull(message = "Venue must have a capacity")
	@Min(value=1, message = "Capacity must be positive")
	private int capacity;
	
	@OneToMany(mappedBy="venue")
	private Set<Event> events = new HashSet<Event>();

	public Venue() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getStreetName() {
		return streetName;
	}
	
	public void setStreetName(String streetName) {
		this.streetName = streetName;
	}
	
	public String getPostcode() {
		return postcode;
	}
	
	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	
	public Set<Event> getEvents() {
		return this.events;
	}
	
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	
	public double getLatitude() {
		return this.latitude;
	}
	
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	
	public double getLongitude() {
		return this.longitude;
	}
	
}
