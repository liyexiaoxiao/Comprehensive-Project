package com.donffroodus.social_service.web;

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

	/** 用于公开读接口：未登录或非法头时返回 null，不抛错。 */
	public static Long optionalUserId(String xUserId) {
		if (xUserId == null || xUserId.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(xUserId.strip());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
