package com.service.gateway.config;

import java.time.Duration;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class WebClientConfig {
	
	@Bean
	@LoadBalanced
	public WebClient.Builder builder(){
		ConnectionProvider provider = ConnectionProvider.builder("fixed")
			    .maxConnections(500)  
			    .maxIdleTime(Duration.ofSeconds(20))  
			    .maxLifeTime(Duration.ofSeconds(60)) 
			    .pendingAcquireTimeout(Duration.ofSeconds(60))  
			    .evictInBackground(Duration.ofSeconds(120)) 
			    .build();
		
		HttpClient httpClient = HttpClient.create(provider)
			    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
			    .responseTimeout(Duration.ofSeconds(10));
		
		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(httpClient));
	}

}
