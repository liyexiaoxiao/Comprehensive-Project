package com.donffroodus.social_service.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.donffroodus.social_service.entity.MoodDiary;
import com.donffroodus.social_service.repository.MoodDiaryRepository;

/**
 * 社交服务 HTTP API：用户情绪日记（mood_diary）的增删改查。
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class SocialApiController {

	private final MoodDiaryRepository moodDiaryRepository;

	public SocialApiController(MoodDiaryRepository moodDiaryRepository) {
		this.moodDiaryRepository = moodDiaryRepository;
	}

	/** 列出当前用户的情绪日记；可选 date 按日期筛选。 */
	@GetMapping("/me/mood-diaries")
	public List<MoodDiary> listUserMoodDiaries(
			@RequestHeader("X-User-Id") String xUserId,
			@RequestParam(value = "date", required = false) LocalDate date) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		if (date != null) {
			return moodDiaryRepository.findByUserIdAndDate(userId, date);
		}
		return moodDiaryRepository.findByUserIdOrderByDateDescCreatedAtDesc(userId);
	}

	/** 按主键获取当前用户的一条情绪日记。 */
	@GetMapping("/me/mood-diaries/{diaryId}")
	public ResponseEntity<MoodDiary> getMoodDiary(
			@RequestHeader("X-User-Id") String xUserId,
			@PathVariable("diaryId") Long diaryId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return moodDiaryRepository.findByIdAndUserId(diaryId, userId)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	/** 新建一条情绪日记。 */
	@PostMapping("/me/mood-diaries")
	public ResponseEntity<MoodDiary> createMoodDiary(
			@RequestHeader("X-User-Id") String xUserId,
			@RequestBody MoodDiaryWriteRequest request) {
		if (request == null || request.date() == null) {
			return ResponseEntity.badRequest().build();
		}

		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		MoodDiary diary = new MoodDiary();
		diary.setUserId(userId);
		diary.setDate(request.date());
		diary.setDominantEmotion(request.dominantEmotion());
		diary.setContext(request.context());
		return ResponseEntity.ok(moodDiaryRepository.save(diary));
	}

	/** 更新当前用户已有的一条情绪日记。 */
	@PutMapping("/me/mood-diaries/{diaryId}")
	public ResponseEntity<MoodDiary> updateMoodDiary(
			@RequestHeader("X-User-Id") String xUserId,
			@PathVariable("diaryId") Long diaryId,
			@RequestBody MoodDiaryWriteRequest request) {
		if (request == null || request.date() == null) {
			return ResponseEntity.badRequest().build();
		}

		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return moodDiaryRepository.findByIdAndUserId(diaryId, userId)
				.map(existing -> {
					existing.setDate(request.date());
					existing.setDominantEmotion(request.dominantEmotion());
					existing.setContext(request.context());
					return ResponseEntity.ok(moodDiaryRepository.save(existing));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	/** 删除当前用户的一条情绪日记。 */
	@DeleteMapping("/me/mood-diaries/{diaryId}")
	public ResponseEntity<Void> deleteMoodDiary(
			@RequestHeader("X-User-Id") String xUserId,
			@PathVariable("diaryId") Long diaryId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return moodDiaryRepository.findByIdAndUserId(diaryId, userId)
				.map(existing -> {
					moodDiaryRepository.delete(existing);
					return ResponseEntity.ok().<Void>build();
				})
				.orElse(ResponseEntity.notFound().build());
	}

	public static record MoodDiaryWriteRequest(
			LocalDate date,
			String dominantEmotion,
			String context) {
	}
}
