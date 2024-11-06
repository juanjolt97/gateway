package com.service.gateway.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import com.service.gateway.dto.TokenDto;

import reactor.core.publisher.Mono;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

	@Value("${base.url.authValidate}")
	String baseUrlAuthString;

	public static class Config {
	}

	private WebClient.Builder webClient;

	public AuthFilter(WebClient.Builder webClient) {
		super(Config.class);
		this.webClient = webClient;
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (((exchange, chain) -> {
			String tokenHeader = null;
			TokenDto tokenDto = null;
			if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION))
				return onError(exchange, HttpStatus.BAD_REQUEST);
			if (exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
				tokenHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
				String[] chunks = tokenHeader.split(" ");
				if (chunks.length != 2 || !chunks[0].equals("Bearer"))
					return onError(exchange, HttpStatus.BAD_REQUEST);
				String token = chunks[1];
				tokenDto = TokenDto.builder().token(token).build();
			}

			if (tokenDto == null) {
				return onError(exchange, HttpStatus.UNAUTHORIZED);
			}

			return webClient.build().post().uri(baseUrlAuthString).bodyValue(tokenDto).retrieve()
					.bodyToMono(TokenDto.class).flatMap(t -> chain.filter(exchange))
					.onErrorResume(error -> {
						return onError(exchange, HttpStatus.UNAUTHORIZED);
					});
		}));
	}

	public Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(httpStatus);
		return response.setComplete();
	}

}
