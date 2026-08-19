package by.yurnerix.msaccountreservation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class MsAccountReservationApplication {

	public static void main(String[] args) {
		var context = SpringApplication.run(MsAccountReservationApplication.class, args);

		log.info(
				"Микросервис {} запустился успешно", context.getEnvironment().getProperty("spring.application.name")
		);
	}
}