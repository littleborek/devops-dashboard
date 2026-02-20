package com.berk.devopsdashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@org.springframework.scheduling.annotation.EnableAsync
public class DevopsdashboardApplication {

	public static void main(String[] args) {
		SpringApplication.run(DevopsdashboardApplication.class, args);
	}

}
