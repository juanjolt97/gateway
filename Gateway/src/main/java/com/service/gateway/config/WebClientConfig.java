package com.service.gateway.config;


import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {
	
	@Bean
	@LoadBalanced
	WebClient.Builder builder(){	
		HttpClient client = HttpClient.create()
				  .option(ChannelOption.SO_KEEPALIVE, true)
				  .option(EpollChannelOption.TCP_KEEPIDLE, 86400)
				  .option(EpollChannelOption.TCP_KEEPINTVL, 60)
				  .option(EpollChannelOption.TCP_KEEPCNT, 8);
		return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(client));
	}

}
