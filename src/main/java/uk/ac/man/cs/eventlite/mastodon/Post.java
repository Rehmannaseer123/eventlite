package uk.ac.man.cs.eventlite.mastodon;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// entity representing mastodon post
public class Post {
	
	// attributes
	private String content;
	private LocalDate date;
	private LocalTime time;
	private URL url;
	
	// getters and setters
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
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
	public String getTimeFormatted() {
		return time.format(DateTimeFormatter.ofPattern("HH:mm"));
	}
	public void setTime(LocalTime time) {
		this.time = time;
	}
	public URL getUrl() {
		return url;
	}
	public void setUrl(URL url) {
		this.url = url;
	}
	
	//constructor
	public Post(String content, LocalDate date, LocalTime time, URL url) {
		super();
		this.content = content;
		this.date = date;
		this.time = time;
		this.url = url;
	}
	@Override
	public String toString() {
		return "Post [content=" + content + ", date=" + date + ", time=" + time + ", url=" + url + "]";
	}
}
