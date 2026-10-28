package pe.upeu.ecorecicla;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
		"DB_PASSWORD=test-only",
		"ecoapi.seed.enabled=false"
})
class EcoapiApplicationTests {

	@Test
	void contextLoads() {
	}

}
