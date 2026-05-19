package com.donffroodus.user_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.donffroodus.user_service.entity.UserInfo;
import com.donffroodus.user_service.repository.UserInfoRepository;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

	@Value("${admin.bootstrap.enabled:false}")
	private boolean enabled;

	@Value("${admin.bootstrap.username:admin}")
	private String username;

	@Value("${admin.bootstrap.password:admin123456}")
	private String password;

	@Value("${admin.bootstrap.force:false}")
	private boolean force;

	@Autowired
	private UserInfoRepository userInfoRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private static String normalizeRole(String role) {
		if (role == null) {
			return "";
		}
		String trimmed = role.trim();
		if (trimmed.isEmpty()) {
			return "";
		}
		String upper = trimmed.toUpperCase();
		return upper.startsWith("ROLE_") ? upper.substring(5) : upper;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!enabled) {
			return;
		}

		String normalizedUsername = username == null ? "" : username.trim();
		if (normalizedUsername.isEmpty()) {
			return;
		}

		userInfoRepository.findByUsername(normalizedUsername).ifPresentOrElse(existing -> {
			boolean changed = false;
			String currentRole = normalizeRole(existing.getRole());
			if (!"ADMIN".equals(currentRole)) {
				existing.setRole("ADMIN");
				changed = true;
			}
			if (force && password != null && !password.isBlank()) {
				existing.setPassword(passwordEncoder.encode(password));
				changed = true;
			}
			if (changed) {
				userInfoRepository.save(existing);
			}
		}, () -> {
			if (password == null || password.isBlank()) {
				return;
			}
			UserInfo created = new UserInfo();
			created.setUsername(normalizedUsername);
			created.setPassword(passwordEncoder.encode(password));
			UserInfo saved = userInfoRepository.save(created);
			saved.setRole("ADMIN");
			userInfoRepository.save(saved);
		});
	}
}

