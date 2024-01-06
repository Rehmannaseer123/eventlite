package uk.ac.man.cs.eventlite.mastodon;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.controllers.EventsController;

class MastodonServiceTest {
	
	private final static Logger log = LoggerFactory.getLogger(MastodonServiceTest.class);

	public static MastodonService testInstance = MastodonService.getInstance();
	
	@Test
	void testReadHome() {
		// publish 3 new statuses to test against
		log.info("Read Home Test - Publishing test statuses");
		testInstance.publishStatus("read test1");
		testInstance.publishStatus("read test2");
		testInstance.publishStatus("read test3");
		try {
			Thread.sleep(1000);
		} catch (Exception e) {}
		
		// call the readHome method
		Iterable<Post> posts = testInstance.readHome();
		try {
			Thread.sleep(1000);
		} catch (Exception e) {}

		// clean up - delete 3 test posts
		log.info("Read Home Test - Deleting test statuses");
		testInstance.deleteLastStatus();
		testInstance.deleteLastStatus();
		testInstance.deleteLastStatus();
		
		// check the returned posts match the 3 test statuses, in reverse order
		Iterator<Post> it = posts.iterator();
		assertThat(it.next().getContent(), equalTo("read test3"));
		assertThat(it.next().getContent(), equalTo("read test2"));
		assertThat(it.next().getContent(), equalTo("read test1"));
	}
	
	@Test
	void testpublishStatus() {
		// call the publishStatus method
		log.info("Publish Status Test - Publishing test status");
		testInstance.publishStatus("publish test");
		try {
			Thread.sleep(1000);
		} catch (Exception e) {}
		
		// read the last 3 posts to check the new status was published correctly
		Iterable<Post> posts = testInstance.readHome();
		try {
			Thread.sleep(1000);
		} catch (Exception e) {}

		// clean up - delete test post
		log.info("Publish Status Test - Deleting test status");
		testInstance.deleteLastStatus();
		
		// check the most recent post matches the test post
		Iterator<Post> it = posts.iterator();
		assertThat(it.next().getContent(), equalTo("publish test"));
	}

}
