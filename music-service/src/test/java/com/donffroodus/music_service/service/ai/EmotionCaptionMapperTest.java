package com.donffroodus.music_service.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class EmotionCaptionMapperTest {

	private final EmotionCaptionMapper mapper = new EmotionCaptionMapper();

	@Test
	void infersJoyFromHappyDescription() {
		List<String> keys = mapper.inferEmotionKeys(
				"A cheerful and upbeat melody with happy piano chords.", 3);
		assertEquals("joy", keys.get(0));
	}

	@Test
	void infersSadnessFromMelancholyDescription() {
		List<String> keys = mapper.inferEmotionKeys(
				"A slow melancholy track with sorrowful strings.", 3);
		assertTrue(keys.contains("sadness"));
	}

	@Test
	void defaultsToNeutralWhenNoKeywordMatch() {
		List<String> keys = mapper.inferEmotionKeys("Instrumental music with rhythm.", 3);
		assertEquals(List.of("neutral"), keys);
	}
}
