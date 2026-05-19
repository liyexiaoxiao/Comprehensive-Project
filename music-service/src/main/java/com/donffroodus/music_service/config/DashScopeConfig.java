package com.donffroodus.music_service.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(DashScopeProperties.class)
public class DashScopeConfig {

	@Bean
	RestClient dashScopeRestClient(DashScopeProperties properties) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(30));
		factory.setReadTimeout(Duration.ofMinutes(3));
		return RestClient.builder()
				.baseUrl(properties.getBaseUrl())
				.requestFactory(factory)
				.build();
	}
}
