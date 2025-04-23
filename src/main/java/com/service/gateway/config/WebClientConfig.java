package com.service.gateway.config;


import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;


@Configuration
public class WebClientConfig {

	@Bean
	@LoadBalanced
	WebClient.Builder builder() {
		// Create a non-pooled HttpClient
        HttpClient httpClient = HttpClient.create(ConnectionProvider.newConnection());
        
		return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
	}

}
