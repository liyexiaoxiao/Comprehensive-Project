package com.donffroodus.music_service.service.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.donffroodus.music_service.entity.EmotionTag;
import com.donffroodus.music_service.entity.MusicResource;
import com.donffroodus.music_service.entity.MusicTagMapping;
import com.donffroodus.music_service.repository.EmotionTagRepository;
import com.donffroodus.music_service.repository.MusicResourceRepository;
import com.donffroodus.music_service.repository.MusicTagMappingRepository;
import com.donffroodus.music_service.service.ai.AudioSourceResolver.ResolvedAudio;

@Service
public class AiEmotionTaggingService {

	private final MusicResourceRepository musicResourceRepository;
	private final EmotionTagRepository emotionTagRepository;
	private final MusicTagMappingRepository musicTagMappingRepository;
	private final AudioSourceResolver audioSourceResolver;
	private final DashScopeAudioCaptionClient captionClient;
	private final EmotionCaptionMapper emotionCaptionMapper;

	public AiEmotionTaggingService(
			MusicResourceRepository musicResourceRepository,
			EmotionTagRepository emotionTagRepository,
			MusicTagMappingRepository musicTagMappingRepository,
			AudioSourceResolver audioSourceResolver,
			DashScopeAudioCaptionClient captionClient,
			EmotionCaptionMapper emotionCaptionMapper) {
		this.musicResourceRepository = musicResourceRepository;
		this.emotionTagRepository = emotionTagRepository;
		this.musicTagMappingRepository = musicTagMappingRepository;
		this.audioSourceResolver = audioSourceResolver;
		this.captionClient = captionClient;
		this.emotionCaptionMapper = emotionCaptionMapper;
	}

	@Transactional
	public AiTaggingResult tagMusicByAi(Long musicId, AiTaggingOptions options) {
		MusicResource music = musicResourceRepository.findById(musicId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Music not found"));

		ResolvedAudio audio = audioSourceResolver.resolve(music.getFileUrl());
		return analyzeResolvedAudio(musicId, audio, options);
	}

	public AiTaggingResult previewTagging(
			String originalFilename,
			String contentType,
			byte[] bytes,
			Integer maxTags,
			String title,
			String artist) {
		int effectiveMaxTags = maxTags != null && maxTags > 0 ? maxTags : 3;
		if (!captionClient.isConfigured()) {
			return analyzeFallbackHints(originalFilename, title, artist, effectiveMaxTags);
		}
		if (bytes == null || bytes.length == 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audio content is empty");
		}
		String mime = normalizeMimeType(originalFilename, contentType);
		ResolvedAudio audio = ResolvedAudio.base64DataUri(bytes, mime);
		return analyzeResolvedAudio(null, audio, new AiTaggingOptions(false, true, effectiveMaxTags));
	}

	private List<EmotionTag> resolveOrCreateTags(List<String> emotionKeys) {
		List<EmotionTag> result = new ArrayList<>();
		Set<Long> seenIds = new HashSet<>();
		for (String key : emotionKeys) {
			EmotionTag tag = findTagForEmotionKey(key)
					.orElseGet(() -> createTagForEmotionKey(key));
			if (seenIds.add(tag.getId())) {
				result.add(tag);
			}
		}
		return result;
	}

	private Optional<EmotionTag> findTagForEmotionKey(String emotionKey) {
		String key = emotionKey.toLowerCase(Locale.ROOT);
		for (String candidate : emotionCaptionMapper.candidateTagNames(key)) {
			Optional<EmotionTag> matched = emotionTagRepository.findByTagName(candidate);
			if (matched.isPresent()) {
				return matched;
			}
		}
		return Optional.empty();
	}

	private EmotionTag createTagForEmotionKey(String emotionKey) {
		String key = emotionKey.toLowerCase(Locale.ROOT);
		EmotionTag tag = new EmotionTag();
		tag.setTagName(emotionCaptionMapper.defaultTagLabel(key));
		return emotionTagRepository.save(tag);
	}

	private List<MusicTagMapping> persistMappings(Long musicId, List<EmotionTag> tags, boolean replaceAiOnly) {
		List<MusicTagMapping> existing = musicTagMappingRepository.findByMusicId(musicId);
		if (replaceAiOnly) {
			List<MusicTagMapping> aiOnly = existing.stream()
					.filter(m -> EmotionCaptionMapper.SOURCE_AI.equalsIgnoreCase(
							m.getSource() != null ? m.getSource() : ""))
					.toList();
			if (!aiOnly.isEmpty()) {
				musicTagMappingRepository.deleteAll(aiOnly);
			}
		} else if (!existing.isEmpty()) {
			musicTagMappingRepository.deleteAll(existing);
		}

		Set<Long> manualTagIds = replaceAiOnly
				? existing.stream()
						.filter(m -> m.getSource() == null
								|| !EmotionCaptionMapper.SOURCE_AI.equalsIgnoreCase(m.getSource()))
						.map(MusicTagMapping::getTagId)
						.collect(Collectors.toCollection(LinkedHashSet::new))
				: Set.of();

		Set<Long> existingTagIds = musicTagMappingRepository.findByMusicId(musicId).stream()
				.map(MusicTagMapping::getTagId)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		List<MusicTagMapping> saved = new ArrayList<>();
		for (EmotionTag tag : tags) {
			if (manualTagIds.contains(tag.getId()) || existingTagIds.contains(tag.getId())) {
				continue;
			}
			MusicTagMapping mapping = new MusicTagMapping();
			mapping.setMusicId(musicId);
			mapping.setTagId(tag.getId());
			mapping.setSource(EmotionCaptionMapper.SOURCE_AI);
			saved.add(musicTagMappingRepository.save(mapping));
		}
		return saved;
	}

	private AiTaggingResult analyzeResolvedAudio(Long musicId, ResolvedAudio audio, AiTaggingOptions options) {
		String caption = captionClient.caption(audio);

		int maxTags = options.maxTags() != null && options.maxTags() > 0 ? options.maxTags() : 3;
		List<String> emotionKeys = emotionCaptionMapper.inferEmotionKeys(caption, maxTags);
		List<EmotionTag> tags = resolveOrCreateTags(emotionKeys);

		List<MusicTagMapping> persisted = List.of();
		if (options.persist()) {
			persisted = persistMappings(musicId, tags, options.replaceAiOnly());
		}

		return new AiTaggingResult(
				musicId,
				caption,
				emotionKeys,
				tags.stream().map(EmotionTag::getId).toList(),
				tags.stream().map(EmotionTag::getTagName).toList(),
				persisted,
				options.persist());
	}

	private String normalizeMimeType(String originalFilename, String contentType) {
		String normalizedContentType = contentType != null ? contentType.strip() : "";
		if (!normalizedContentType.isEmpty() && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(normalizedContentType)) {
			return normalizedContentType;
		}
		return AudioSourceResolver.guessMimeFromPath(originalFilename);
	}

	private AiTaggingResult analyzeFallbackHints(String originalFilename, String title, String artist, int maxTags) {
		String hint = buildFallbackHint(originalFilename, title, artist);
		List<String> emotionKeys = emotionCaptionMapper.inferEmotionKeysFromHint(hint, maxTags);
		List<EmotionTag> tags = resolveOrCreateTags(emotionKeys);
		String caption = hint.isBlank()
				? "DashScope 未配置，已使用默认兜底标签。"
				: "DashScope 未配置，已根据歌曲信息做兜底识别。";
		return new AiTaggingResult(
				null,
				caption,
				emotionKeys,
				tags.stream().map(EmotionTag::getId).toList(),
				tags.stream().map(EmotionTag::getTagName).toList(),
				List.of(),
				false);
	}

	private String buildFallbackHint(String originalFilename, String title, String artist) {
		List<String> parts = new ArrayList<>();
		if (title != null && !title.isBlank()) {
			parts.add(title.strip());
		}
		if (artist != null && !artist.isBlank()) {
			parts.add(artist.strip());
		}
		if (originalFilename != null && !originalFilename.isBlank()) {
			parts.add(originalFilename.replaceFirst("\\.[^.]+$", "").strip());
		}
		return String.join(" ", parts);
	}

	public record AiTaggingOptions(boolean persist, boolean replaceAiOnly, Integer maxTags) {

		public static AiTaggingOptions defaults() {
			return new AiTaggingOptions(true, true, 3);
		}
	}

	public record AiTaggingResult(
			Long musicId,
			String caption,
			List<String> inferredEmotionKeys,
			List<Long> tagIds,
			List<String> tagNames,
			List<MusicTagMapping> mappings,
			boolean persisted) {
	}
}
