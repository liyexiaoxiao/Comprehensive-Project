package com.donffroodus.api_gateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import com.donffroodus.api_gateway.utils.JwtUtils;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtils jwtUtils;

    private static final String[] WHITELIST = {
        "/api/users/login",
        "/api/users/register",
        "/api/users/avatars"
    };

    private boolean isInWhitelist(String path) {
        for (String whitelistedPath : WHITELIST) {
            if (matchesWhitelistedPath(path, whitelistedPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesWhitelistedPath(String path, String whitelistedPath) {
        return path.equals(whitelistedPath)
                || path.equals(whitelistedPath + "/")
                || path.startsWith(whitelistedPath + "/")
                || path.startsWith(whitelistedPath + "?")
                || path.startsWith(whitelistedPath + "/?")
                || (path + "/").equals(whitelistedPath);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isInWhitelist(path)) {
            return chain.filter(exchange);
        }

        String token = parseJwt(request);

        if (token == null || !jwtUtils.validateToken(token)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED); // 401 拦截
            return response.setComplete();
        }

        try {
            Claims claims = jwtUtils.getClaimsFromToken(token);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            Long userId = claims.get("userId", Long.class);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Name");
                        headers.remove("X-User-Role");
                        headers.remove("X-User-Id");
                        headers.set("X-User-Name", username);
                        headers.set("X-User-Role", "ROLE_" + role.toUpperCase()); // 提前把 ROLE_ 拼好
                        headers.set("X-User-Id", userId.toString());
                    })
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
    }

    private String parseJwt(ServerHttpRequest request) {
        String headerAuth = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    @Override
    public int getOrder() {
        // 优先级设置：数字越小，执行越早。设为 -1 确保它在路由转发前执行。
        return -1;
    }
}
