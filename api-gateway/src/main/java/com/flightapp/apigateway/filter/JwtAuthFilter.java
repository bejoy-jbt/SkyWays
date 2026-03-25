package com.flightapp.apigateway.filter;

import com.flightapp.apigateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            // ✅ Allow Swagger & OpenAPI endpoints WITHOUT JWT
            if (path.contains("/v3/api-docs") ||
                path.contains("/swagger-ui") ||
                path.contains("/swagger-ui.html") ||
                path.contains("/webjars")) {
                return chain.filter(exchange);
            }
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.isValid(token)) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            // Admin-only routes check
            
            if (path.startsWith("/api/admin") && !"ADMIN".equals(jwtUtil.extractRole(token))) {
                return onError(exchange, HttpStatus.FORBIDDEN);
            }

            // Forward user info downstream via headers
            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Email", jwtUtil.extractUsername(token))
                    .header("X-User-Role",  jwtUtil.extractRole(token))
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

    public static class Config {}
}
