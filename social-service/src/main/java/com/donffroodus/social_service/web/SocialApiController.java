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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.donffroodus.social_service.entity.MoodDiary;
import com.donffroodus.social_service.repository.MoodDiaryRepository;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class SocialApiController {

	private final MoodDiaryRepository moodDiaryRepository;

	public SocialApiController(MoodDiaryRepository moodDiaryRepository) {
		this.moodDiaryRepository = moodDiaryRepository;
	}

	@GetMapping("/users/{userId}/mood-diaries")
	public List<MoodDiary> listUserMoodDiaries(
			@PathVariable("userId") Long userId,
			@RequestParam(value = "date", required = false) LocalDate date) {
		if (date != null) {
			return moodDiaryRepository.findByUserIdAndDate(userId, date);
		}
		return moodDiaryRepository.findByUserIdOrderByDateDescCreatedAtDesc(userId);
	}

	@GetMapping("/users/{userId}/mood-diaries/{diaryId}")
	public ResponseEntity<MoodDiary> getMoodDiary(
			@PathVariable("userId") Long userId,
			@PathVariable("diaryId") Long diaryId) {
		return moodDiaryRepository.findByIdAndUserId(diaryId, userId)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/users/{userId}/mood-diaries")
	public ResponseEntity<MoodDiary> createMoodDiary(
			@PathVariable("userId") Long userId,
			@RequestBody MoodDiaryWriteRequest request) {
		if (request == null || request.date() == null) {
			return ResponseEntity.badRequest().build();
		}

		MoodDiary diary = new MoodDiary();
		diary.setUserId(userId);
		diary.setDate(request.date());
		diary.setDominantEmotion(request.dominantEmotion());
		diary.setContext(request.context());
		return ResponseEntity.ok(moodDiaryRepository.save(diary));
	}

	@PutMapping("/users/{userId}/mood-diaries/{diaryId}")
	public ResponseEntity<MoodDiary> updateMoodDiary(
			@PathVariable("userId") Long userId,
			@PathVariable("diaryId") Long diaryId,
			@RequestBody MoodDiaryWriteRequest request) {
		if (request == null || request.date() == null) {
			return ResponseEntity.badRequest().build();
		}

		return moodDiaryRepository.findByIdAndUserId(diaryId, userId)
				.map(existing -> {
					existing.setDate(request.date());
					existing.setDominantEmotion(request.dominantEmotion());
					existing.setContext(request.context());
					return ResponseEntity.ok(moodDiaryRepository.save(existing));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/users/{userId}/mood-diaries/{diaryId}")
	public ResponseEntity<Void> deleteMoodDiary(
			@PathVariable("userId") Long userId,
			@PathVariable("diaryId") Long diaryId) {
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
