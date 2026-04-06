package com.donffroodus.music_service.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * API Gateway strips client-supplied identity headers and sets {@code X-User-Id} from the JWT.
 */
public final class GatewayAuthSupport {

	private GatewayAuthSupport() {
	}

	public static Long requireUserId(String xUserId) {
		if (xUserId == null || xUserId.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id");
		}
		try {
			return Long.parseLong(xUserId.strip());
		} catch (NumberFormatException e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid X-User-Id");
		}
	}
}
