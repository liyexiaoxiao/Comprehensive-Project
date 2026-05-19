package com.donffroodus.music_service.service.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 将 Captioner 输出的英文描述映射为项目情绪键（与 Python / 前端一致）。
 */
@Component
public class EmotionCaptionMapper {

	public static final String SOURCE_AI = "ai";

	private static final Map<String, List<String>> EMOTION_KEYWORDS = Map.ofEntries(
			Map.entry("joy", List.of("joy", "happy", "happiness", "cheerful", "upbeat", "delight", "celebration",
					"glad", "playful", "bright", "uplifting", "excited", "energetic")),
			Map.entry("sadness", List.of("sad", "sadness", "sorrow", "melancholy", "gloomy", "grief", "lonely",
					"depressing", "tearful", "mournful", "heartbroken")),
			Map.entry("anger", List.of("anger", "angry", "rage", "furious", "aggressive", "hostile", "irritated",
					"frustrated")),
			Map.entry("fear", List.of("fear", "afraid", "anxious", "anxiety", "scary", "terrifying", "tense",
					"nervous", "uneasy", "horror", "suspense")),
			Map.entry("love", List.of("love", "romantic", "affection", "tender", "passion", "intimate", "warmth")),
			Map.entry("surprise", List.of("surprise", "surprising", "unexpected", "sudden", "astonishing")),
			Map.entry("neutral", List.of("calm", "peaceful", "relaxing", "neutral", "ambient", "gentle", "soft",
					"soothing", "meditative", "serene", "quiet", "background")));

	/** 情绪键 → 写入 emotion_tag.tag_name 的默认中文名 */
	private static final Map<String, String> DEFAULT_TAG_LABELS = Map.of(
			"joy", "喜悦",
			"sadness", "悲伤",
			"anger", "愤怒",
			"fear", "恐惧",
			"love", "爱",
			"surprise", "惊喜",
			"neutral", "平静");

	/**
	 * @param maxTags 最多返回的情绪数量
	 */
	public List<String> inferEmotionKeys(String caption, int maxTags) {
		if (caption == null || caption.isBlank()) {
			return List.of("neutral");
		}
		String text = caption.toLowerCase(Locale.ROOT);
		Map<String, Integer> scores = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : EMOTION_KEYWORDS.entrySet()) {
			int score = 0;
			for (String keyword : entry.getValue()) {
				if (text.contains(keyword)) {
					score++;
				}
			}
			if (score > 0) {
				scores.put(entry.getKey(), score);
			}
		}
		if (scores.isEmpty()) {
			return List.of("neutral");
		}
		List<String> ranked = new ArrayList<>(scores.keySet());
		ranked.sort(Comparator.comparingInt((String k) -> scores.get(k)).reversed());
		int limit = Math.max(1, maxTags);
		return ranked.size() <= limit ? ranked : ranked.subList(0, limit);
	}

	public String defaultTagLabel(String emotionKey) {
		return DEFAULT_TAG_LABELS.getOrDefault(emotionKey, emotionKey);
	}

	public static Map<String, String> defaultTagLabels() {
		return DEFAULT_TAG_LABELS;
	}
}
