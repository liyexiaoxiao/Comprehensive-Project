package com.donffroodus.social_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.donffroodus.social_service.config.AiServiceProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiServiceProperties.class)
public class SocialServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SocialServiceApplication.class, args);
	}
}
