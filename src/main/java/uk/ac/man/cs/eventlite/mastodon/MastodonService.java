package uk.ac.man.cs.eventlite.mastodon;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.google.gson.Gson;
import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Statuses;
import com.sys1yagi.mastodon4j.api.method.Timelines;

import okhttp3.OkHttpClient;

// singleton class use to access external mastodon service
public class MastodonService {
	
	private final static Logger log = LoggerFactory.getLogger(MastodonService.class);
	
	private static MastodonService instance;
	
	private final static String ACCESS_TOKEN = "WmNO2vVJnkTDrIjzVZR2_cHzM55s4mtZTq622UZXN_o";
	private final static String DISPLAY_NAME = "byte_me_now";
	
	private MastodonClient client;

	
	private MastodonService() {
		register();
	}
	
	// returns the singleton instance of the class
	public static MastodonService getInstance() {
		if (instance == null) {
			instance = new MastodonService();
		}
		return instance;
	}
	
	
	// setup client with mastodon instance and the correct access token
	public void register() {
		client = new MastodonClient.Builder("mastodonapp.uk", new OkHttpClient.Builder(), new Gson())
				.accessToken(ACCESS_TOKEN)
				.build();
	}
	
	
	// returns list of last 3 posts from the EventLite account
	public Iterable<Post> readHome() {
		Timelines timelines = new Timelines(client);
		ArrayList<Post> posts = new ArrayList<Post>();

		try {
			// gets the home time line as a list of statuses
			List<Status> statuses = timelines.getHome().execute().getPart();
			
			for (Status status: statuses) {
				// checks if the status is from the correct account
				if (status.getAccount().getDisplayName().equals(DISPLAY_NAME)) {
					// removes ASCII characters and HTML tags from the content
					String tempContent = HtmlUtils.htmlUnescape(Jsoup.clean(status.getContent(), Safelist.none()));
					// converts string URL to object URL
					URL tempURL = null;
					try {
						tempURL = new URL(status.getUrl());
					} catch (MalformedURLException e) {
						log.info("Invalid URL: " + status.getUrl());
					}
					// gets date section of status DateTime
					LocalDate tempDate = LocalDate.parse(status.getCreatedAt().subSequence(0, 10));
					// gets time section of status DateTime
					LocalTime tempTime = LocalTime.parse(status.getCreatedAt().subSequence(11, 20));
					
					// adds status to output list
					posts.add(new Post(tempContent, tempDate, tempTime, tempURL));
					
					// stops looking after 3 posts have been found
					if (posts.size() >= 3) {
						break;
					}
				}
			}
		} catch (Mastodon4jRequestException e) {
		  	log.info("Mastodon read timeline error: [code " + e.getResponse().code() + "]");
		}
		
		return posts;
	}
	
	
	// publishes a message to mastodon with the given message text
	public void publishStatus(String text) {
		Statuses statuses = new Statuses(client);
		try {
			statuses.postStatus(text, null, null, false, null, Status.Visibility.Unlisted).execute();
		} catch (Mastodon4jRequestException e) {
		  	log.info("Mastodon post status error: [code " + e.getResponse().code() + "]");
		}
	}
	
	
	// deletes the most recent status -- only used for testing
	public void deleteLastStatus() {
		// gets id from the URL of most recent post
		Post status = readHome().iterator().next();
		String[] urlPath = status.getUrl().getPath().split("/");
		long statusId = Long.parseLong(urlPath[urlPath.length - 1]);
		
		// delete status with the found ID
		Statuses statuses = new Statuses(client);
		try {
			statuses.deleteStatus(statusId);
		} catch (Mastodon4jRequestException e) {
		  	log.info("Mastodon delete status error: [code " + e.getResponse().code() + "]");
		}
	}
}
