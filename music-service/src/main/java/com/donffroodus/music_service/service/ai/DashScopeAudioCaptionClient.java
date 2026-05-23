package com.donffroodus.music_service.service.ai;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.donffroodus.music_service.config.DashScopeProperties;
import com.donffroodus.music_service.service.ai.AudioSourceResolver.ResolvedAudio;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 调用 Qwen3-Omni-Captioner（OpenAI 兼容 Chat Completions）生成英文音频描述。
 */
@Service
public class DashScopeAudioCaptionClient {

	private final DashScopeProperties properties;
	private final RestClient dashScopeRestClient;

	public DashScopeAudioCaptionClient(DashScopeProperties properties, RestClient dashScopeRestClient) {
		this.properties = properties;
		this.dashScopeRestClient = dashScopeRestClient;
	}

	public String caption(ResolvedAudio audio) {
		if (!properties.isConfigured()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"DASHSCOPE_API_KEY is not configured");
		}

		Map<String, Object> body = Map.of(
				"model", properties.getModel(),
				"messages", List.of(Map.of(
						"role", "user",
						"content", List.of(Map.of(
								"type", "input_audio",
								"input_audio", Map.of("data", audio.payloadForApi()))))));

		try {
			ChatCompletionResponse response = dashScopeRestClient.post()
					.uri("/chat/completions")
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + properties.getApiKey())
					.body(body)
					.retrieve()
					.body(ChatCompletionResponse.class);

			if (response == null || response.choices == null || response.choices.isEmpty()) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty caption response from DashScope");
			}
			ChatChoice choice = response.choices.get(0);
			if (choice.message == null || choice.message.content == null || choice.message.content.isBlank()) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Caption content is empty");
			}
			return choice.message.content.strip();
		} catch (ResponseStatusException ex) {
			throw ex;
		} catch (RestClientResponseException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
					"DashScope API error: " + ex.getStatusCode() + " " + ex.getResponseBodyAsString(), ex);
		} catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to call DashScope caption API", ex);
		}
	}

	public boolean isConfigured() {
		return properties.isConfigured();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	static class ChatCompletionResponse {
		@JsonProperty("choices")
		List<ChatChoice> choices;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	static class ChatChoice {
		@JsonProperty("message")
		ChatMessage message;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	static class ChatMessage {
		@JsonProperty("content")
		String content;
	}
}
