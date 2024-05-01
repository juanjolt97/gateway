package com.service.gateway.config;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import com.service.gateway.dto.TokenDto;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config>{
	
	public static class Config{}
	
	private WebClient.Builder webClient;
	
	public AuthFilter(WebClient.Builder webClient) {
		super(Config.class);
		this.webClient = webClient;
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (((exchange, chain)->{
			String tokenHeader = null;
			TokenDto tokenDto = null;
			if(!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION))
				return onError(exchange, HttpStatus.BAD_REQUEST);
			if(exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
				tokenHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
				String [] chunks = tokenHeader.split(" ");
				if(chunks.length!=2 || !chunks[0].equals("Bearer"))
					return onError(exchange, HttpStatus.BAD_REQUEST);
				String token = chunks[1];
				tokenDto = TokenDto.builder().token(token).build();
			}
			
			if (exchange.getRequest().getHeaders().containsKey(HttpHeaders.COOKIE)) {
	            List<String> cookies = exchange.getRequest().getHeaders().get(HttpHeaders.COOKIE);
	            for (String cookieHeader : cookies) {
	                String[] chunks = cookieHeader.split(";");
	                for (String chunk : chunks) {
	                    String[] keyValue = chunk.trim().split("=");
	                    if (keyValue.length == 2 && keyValue[0].equals("accessToken")) {
	                        String token = keyValue[1];
	                        tokenDto = TokenDto.builder().token(token).build();
	                    }
	                }
	            }
	        }
			
			if (tokenDto == null) {
	            return onError(exchange, HttpStatus.UNAUTHORIZED);
	        }
			
			return webClient.build()
					.post()
					.uri("http://auth-service/auth/validate")
					.bodyValue(tokenDto)
					.retrieve()
					.bodyToMono(TokenDto.class)
					.map(t -> {
						t.getToken();
						return exchange;
					}).flatMap(chain::filter);
		}));
	}
	
	public Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus){
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(httpStatus);
		return response.setComplete();
	}

}