package com.utp.restacontrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RestacontrolApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestacontrolApplication.class, args);
	}

	

}
