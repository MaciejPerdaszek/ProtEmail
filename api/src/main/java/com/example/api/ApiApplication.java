package com.example.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.github.cdimascio.dotenv.Dotenv;

@EnableScheduling
@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {

		Dotenv dotenv = Dotenv.load();

		System.setProperty("GOOGLE_SAFE_BROWSING_API_KEY", dotenv.get("GOOGLE_SAFE_BROWSING_API_KEY"));
		System.setProperty("MYSQL_DB", dotenv.get("MYSQL_DB"));
		System.setProperty("MYSQL_HOST", dotenv.get("MYSQL_HOST"));
		System.setProperty("MYSQL_PASSWORD", dotenv.get("MYSQL_PASSWORD"));
		System.setProperty("MYSQL_PORT", dotenv.get("MYSQL_PORT"));
		System.setProperty("MYSQL_USER", dotenv.get("MYSQL_USER"));
		System.setProperty("OKTA_CLIENT_SECRET", dotenv.get("OKTA_CLIENT_SECRET"));
		System.setProperty("OKTA_MANAGEMENT_CLIENT_SECRET", dotenv.get("OKTA_MANAGEMENT_CLIENT_SECRET"));
		System.setProperty("SECRET_KEY", dotenv.get("SECRET_KEY"));
		System.setProperty("URLSCAN_IO_API_KEY", dotenv.get("URLSCAN_IO_API_KEY"));

		SpringApplication.run(ApiApplication.class, args);
	}

}
