package ai.freightfox.doc.storage.service.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "aws.access.key=test-key",
    "aws.secret.key=test-secret",
    "aws.s3.region=ap-south-1", 
    "aws.s3.bucket.name=test-bucket"
})
class FFoxDocStorageServiceApplicationTests {

	// Test application context loads successfully with test properties
	@Test
	void contextLoads() {
	}

}
