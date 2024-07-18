package com.service.gateway.config;


import java.nio.charset.StandardCharsets;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.handler.logging.LogLevel;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@Configuration
public class WebClientConfig {
	
	@Bean
	@LoadBalanced
	WebClient.Builder builder(){	
		HttpClient client = HttpClient.create()
				  .option(ChannelOption.SO_KEEPALIVE, true)
				  .option(EpollChannelOption.TCP_KEEPIDLE, 300)
				  .option(EpollChannelOption.TCP_KEEPINTVL, 60)
				  .option(EpollChannelOption.TCP_KEEPCNT, 8)
				  .wiretap(HttpClient.class.getCanonicalName(), LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL, StandardCharsets.UTF_8);;
		return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(client));
	}

}
