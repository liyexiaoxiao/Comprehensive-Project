package com.donffroodus.music_service.service.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 将 Captioner 输出的英文描述映射为项目音乐模块统一的 8 类情绪键。
 */
@Component
public class EmotionCaptionMapper {

	public static final String SOURCE_AI = "ai";

	private static final Map<String, List<String>> EMOTION_KEYWORDS = Map.ofEntries(
			Map.entry("joy", List.of("joy", "happy", "happiness", "cheerful", "upbeat", "delight", "celebration",
					"glad", "playful", "bright", "uplifting", "excited", "energetic", "romantic", "affection",
					"tender", "passion", "warmth", "sweet")),
			Map.entry("sadness", List.of("sad", "sadness", "sorrow", "melancholy", "gloomy", "grief", "lonely",
					"depressing", "tearful", "mournful", "heartbroken", "blue", "downcast")),
			Map.entry("anger", List.of("anger", "angry", "rage", "furious", "aggressive", "hostile", "irritated",
					"frustrated", "intense", "explosive")),
			Map.entry("fear", List.of("fear", "afraid", "anxious", "anxiety", "scary", "terrifying", "tense",
					"nervous", "uneasy", "horror", "suspense", "panic", "stress")),
			Map.entry("disgust", List.of("disgust", "disgusting", "repulsive", "revolting", "nauseating",
					"disturbing", "harsh", "dirty", "aversion", "contempt")),
			Map.entry("calm", List.of("calm", "peaceful", "relaxing", "soothing", "meditative", "serene", "healing",
					"relieved", "restful", "tranquil", "comforting", "gentle", "soft", "focus", "focused")),
			Map.entry("surprise", List.of("surprise", "surprising", "unexpected", "sudden", "astonishing",
					"dramatic", "shocking", "wow")),
			Map.entry("neutral", List.of("neutral", "ambient", "background", "steady", "plain", "balanced", "simple",
					"minimal", "everyday", "ordinary", "moderate", "consistent")));

	/** 情绪键 -> 写入 emotion_tag.tag_name 的首选中文名（与系统 8 类情绪对齐）。 */
	private static final Map<String, String> DEFAULT_TAG_LABELS = Map.of(
			"joy", "高兴",
			"sadness", "悲伤",
			"anger", "愤怒",
			"fear", "恐惧",
			"disgust", "厌恶",
			"calm", "平静",
			"surprise", "惊讶",
			"neutral", "中性");

	/**
	 * 兼容历史标签名、前端中文文案和英文键，避免升级后重复创建近义标签。
	 */
	private static final Map<String, List<String>> TAG_NAME_CANDIDATES = Map.ofEntries(
			Map.entry("joy", List.of("高兴", "喜悦", "快乐", "开心", "joy")),
			Map.entry("sadness", List.of("悲伤", "sadness", "sad", "孤独")),
			Map.entry("anger", List.of("愤怒", "anger", "angry")),
			Map.entry("fear", List.of("恐惧", "fear", "焦虑", "anxiety")),
			Map.entry("disgust", List.of("厌恶", "disgust")),
			Map.entry("calm", List.of("平静", "calm", "放松", "舒缓")),
			Map.entry("surprise", List.of("惊讶", "惊喜", "surprise")),
			Map.entry("neutral", List.of("中性", "neutral")));

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

	public List<String> inferEmotionKeysFromHint(String hint, int maxTags) {
		if (hint == null || hint.isBlank()) {
			return List.of("neutral");
		}
		String text = hint.toLowerCase(Locale.ROOT);
		Map<String, Integer> scores = new LinkedHashMap<>();
		for (String emotionKey : DEFAULT_TAG_LABELS.keySet()) {
			int score = 0;
			for (String keyword : candidateTagNames(emotionKey)) {
				String normalizedKeyword = String.valueOf(keyword).toLowerCase(Locale.ROOT);
				if (!normalizedKeyword.isBlank() && text.contains(normalizedKeyword)) {
					score++;
				}
			}
			if (score > 0) {
				scores.put(emotionKey, score);
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

	public List<String> candidateTagNames(String emotionKey) {
		String key = String.valueOf(emotionKey).toLowerCase(Locale.ROOT);
		LinkedHashSet<String> candidates = new LinkedHashSet<>();
		candidates.add(key);
		candidates.add(defaultTagLabel(key));
		candidates.addAll(TAG_NAME_CANDIDATES.getOrDefault(key, List.of()));
		return new ArrayList<>(candidates);
	}

	public static Map<String, String> defaultTagLabels() {
		return DEFAULT_TAG_LABELS;
	}
}
