package com.donffroodus.music_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dashscope")
public class DashScopeProperties {

	/**
	 * 阿里云百炼 API Key（环境变量 DASHSCOPE_API_KEY）。
	 */
	private String apiKey = "";

	/**
	 * OpenAI 兼容模式 Base URL（北京地域）。
	 */
	private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

	private String model = "qwen3-omni-30b-a3b-captioner";

	/** 拉取/读取音频时的最大字节数（与文档 10MB 限制一致）。 */
	private long maxAudioBytes = 10L * 1024 * 1024;

	/**
	 * 解析 fileUrl 时，若为本机地址则先下载再以 Base64 提交（Captioner 无法访问 localhost）。
	 */
	private boolean fetchLocalhostAudio = true;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public long getMaxAudioBytes() {
		return maxAudioBytes;
	}

	public void setMaxAudioBytes(long maxAudioBytes) {
		this.maxAudioBytes = maxAudioBytes;
	}

	public boolean isFetchLocalhostAudio() {
		return fetchLocalhostAudio;
	}

	public void setFetchLocalhostAudio(boolean fetchLocalhostAudio) {
		this.fetchLocalhostAudio = fetchLocalhostAudio;
	}

	public boolean isConfigured() {
		return apiKey != null && !apiKey.isBlank();
	}
}
