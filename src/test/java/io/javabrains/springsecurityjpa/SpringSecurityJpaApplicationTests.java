package io.javabrains.springsecurityjpa;

import io.javabrains.springsecurityjpa.models.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringSecurityJpaApplicationTests {

	@Test
	public void testUser() {
		User user = new User("123@example.com", "test", "123", "dsgsd");
		assertEquals("123@example.com", user.getUserName());
	}

}
