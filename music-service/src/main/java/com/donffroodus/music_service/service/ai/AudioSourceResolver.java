package com.donffroodus.music_service.service.ai;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.donffroodus.music_service.config.DashScopeProperties;

/**
 * 将曲库 {@code fileUrl} 解析为可提交给 Captioner 的音频载荷（公网 URL 或 Base64 Data URI）。
 */
@Component
public class AudioSourceResolver {

	private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "0.0.0.0", "::1");

	private final DashScopeProperties properties;
	private final RestClient restClient;

	public AudioSourceResolver(DashScopeProperties properties) {
		this.properties = properties;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(15));
		factory.setReadTimeout(Duration.ofMinutes(2));
		this.restClient = RestClient.builder().requestFactory(factory).build();
	}

	public ResolvedAudio resolve(String fileUrl) {
		if (fileUrl == null || fileUrl.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileUrl is empty");
		}
		String trimmed = fileUrl.strip();

		if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
			return resolveHttp(trimmed);
		}
		if (trimmed.startsWith("file://")) {
			return resolveLocalFile(Path.of(URI.create(trimmed)));
		}
		Path path = Path.of(trimmed);
		if (Files.isRegularFile(path)) {
			return resolveLocalFile(path);
		}
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported fileUrl: " + trimmed);
	}

	private ResolvedAudio resolveHttp(String url) {
		URI uri = URI.create(url);
		String host = uri.getHost();
		if (host != null && properties.isFetchLocalhostAudio() && isLocalHost(host.toLowerCase(Locale.ROOT))) {
			byte[] bytes = fetchBytes(url);
			return ResolvedAudio.base64DataUri(bytes, guessMimeFromPath(uri.getPath()));
		}
		return ResolvedAudio.publicUrl(url);
	}

	private ResolvedAudio resolveLocalFile(Path path) {
		try {
			if (!Files.isRegularFile(path)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audio file not found: " + path);
			}
			long size = Files.size(path);
			if (size > properties.getMaxAudioBytes()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Audio file exceeds max size (" + properties.getMaxAudioBytes() + " bytes)");
			}
			byte[] bytes = Files.readAllBytes(path);
			return ResolvedAudio.base64DataUri(bytes, guessMimeFromPath(path.toString()));
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read audio file", e);
		}
	}

	private byte[] fetchBytes(String url) {
		try {
			byte[] body = restClient.get()
					.uri(url)
					.retrieve()
					.body(byte[].class);
			if (body == null || body.length == 0) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty audio response from fileUrl");
			}
			if (body.length > properties.getMaxAudioBytes()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Audio exceeds max size (" + properties.getMaxAudioBytes() + " bytes)");
			}
			return body;
		} catch (ResponseStatusException ex) {
			throw ex;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch audio from fileUrl", e);
		}
	}

	private static boolean isLocalHost(String host) {
		return LOCAL_HOSTS.contains(host) || host.endsWith(".localhost");
	}

	static String guessMimeFromPath(String path) {
		if (path == null) {
			return "audio/mpeg";
		}
		String lower = path.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".wav")) {
			return "audio/wav";
		}
		if (lower.endsWith(".aac")) {
			return "audio/aac";
		}
		if (lower.endsWith(".m4a")) {
			return "audio/mp4";
		}
		if (lower.endsWith(".flac")) {
			return "audio/flac";
		}
		return "audio/mpeg";
	}

	public sealed interface ResolvedAudio permits PublicUrlAudio, Base64DataUriAudio {

		String payloadForApi();

		static PublicUrlAudio publicUrl(String url) {
			return new PublicUrlAudio(url);
		}

		static Base64DataUriAudio base64DataUri(byte[] bytes, String mime) {
			if (bytes.length == 0) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audio content is empty");
			}
			String encoded = Base64.getEncoder().encodeToString(bytes);
			String dataUri = "data:" + mime + ";base64," + encoded;
			return new Base64DataUriAudio(dataUri);
		}
	}

	public record PublicUrlAudio(String url) implements ResolvedAudio {
		@Override
		public String payloadForApi() {
			return url;
		}
	}

	public record Base64DataUriAudio(String dataUri) implements ResolvedAudio {
		@Override
		public String payloadForApi() {
			return dataUri;
		}
	}
}
