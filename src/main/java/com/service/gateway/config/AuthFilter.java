package com.service.gateway.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import com.github.benmanes.caffeine.cache.Cache;
import com.service.gateway.dto.TokenDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

	@Value("${base.url.authValidate}")
	String baseUrlAuthString;

	public static class Config {
		 private String someProperty;

		    public String getSomeProperty() {
		        return someProperty;
		    }

		    public void setSomeProperty(String someProperty) {
		        this.someProperty = someProperty;
		    }

	}
	
	private Cache<String, Boolean> tokenCache;

	private WebClient.Builder webClient;

	public AuthFilter(WebClient.Builder webClient, Cache<String, Boolean> tokenCache) {
		super(Config.class);
		this.webClient = webClient;
		this.tokenCache = tokenCache;
	}


	@Override
	public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, HttpStatus.BAD_REQUEST);
            }

            String tokenHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.BAD_REQUEST);
            }

            String token = tokenHeader.substring(7); // Eliminar "Bearer "

            // Verificar si el token está en caché
            if (tokenCache.getIfPresent(token) != null) {
                return chain.filter(exchange); // 🔥 Saltamos la validación porque ya es válido
            }

            // Si no está en caché, validamos con auth-service
            return webClient.build()
                    .post()
                    .uri(baseUrlAuthString)
                    .bodyValue(new TokenDto(token))
                    .retrieve()
                    .bodyToMono(TokenDto.class)
                    .flatMap(t -> {     	
                        tokenCache.put(token, true); // Guardar en caché el token válido
                        return chain.filter(exchange);
                    })
                    .onErrorResume(error -> onError(exchange, HttpStatus.UNAUTHORIZED));
        });
    }

	public Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(httpStatus);
		return response.setComplete();
	}

}
