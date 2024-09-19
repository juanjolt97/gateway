package com.service.gateway.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

@Configuration
@Slf4j
public class WebClientConfig {

	@Bean
	@LoadBalanced
	WebClient.Builder builder() {

		HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
				.option(ChannelOption.SO_KEEPALIVE, true).option(EpollChannelOption.TCP_KEEPIDLE, 300)
				.option(EpollChannelOption.TCP_KEEPINTVL, 60).option(EpollChannelOption.TCP_KEEPCNT, 8);
		// Aumentar timeout de respuesta
		return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient))
				.filter(retryWhenConnectionReset());
	}

	private ExchangeFilterFunction retryWhenConnectionReset() {
		return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
			return Mono.just(clientRequest)
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(5))
							.filter(throwable -> throwable.getMessage() != null
									&& throwable.getMessage().contains("Connection reset by peer"))
							.doAfterRetry(c -> log.info("Reintentando conexión...")));
		});
	}

}
