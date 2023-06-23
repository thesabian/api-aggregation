package com.fedex.assessment.dsabia.apiaggregation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@Configuration
@EnableAsync
public class ApiAggregationApplication {

	@Bean
	ExecutorService executor() {
		return Executors.newFixedThreadPool(100);
	}

	public static void main(String[] args) {
		SpringApplication.run(ApiAggregationApplication.class, args);
	}

}
