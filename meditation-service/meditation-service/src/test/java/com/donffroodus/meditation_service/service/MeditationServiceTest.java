package com.donffroodus.meditation_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.donffroodus.meditation_service.dto.MeditationCountDownStartRequest;
import com.donffroodus.meditation_service.dto.MeditationRequest;
import com.donffroodus.meditation_service.entity.MeditationLog;
import com.donffroodus.meditation_service.entity.MeditationRecord;
import com.donffroodus.meditation_service.entity.MeditationSession;
import com.donffroodus.meditation_service.repository.MeditationLogRepository;
import com.donffroodus.meditation_service.repository.MeditationRecordRepository;
import com.donffroodus.meditation_service.repository.MeditationSessionRepository;

@ExtendWith(MockitoExtension.class)
class MeditationServiceTest {

    @Mock
    private MeditationLogRepository meditationLogRepository;

    @Mock
    private MeditationRecordRepository meditationRecordRepository;

    @Mock
    private MeditationSessionRepository meditationSessionRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private MeditationService meditationService;

    @Test
    void getMediListByUserIdShouldReturnLogsOrderedByRepository() {
        List<MeditationLog> logs = List.of(log(1L, 7L), log(2L, 7L));
        when(meditationLogRepository.findByUserIdOrderByStartTimeDesc(7L)).thenReturn(logs);

        List<MeditationLog> result = meditationService.getMediListByUserId(7L);

        assertEquals(logs, result);
    }

    @Test
    void saveMeditationLogShouldPersistRequestFields() {
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 18, 10, 0);
        MeditationRequest request = new MeditationRequest();
        request.setStartTime(startTime);
        request.setDuration(20);
        request.setMusicId("music-1");
        request.setImageId(3L);
        when(meditationLogRepository.save(any(MeditationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MeditationLog saved = meditationService.saveMeditationLog(7L, request);

        assertEquals(7L, saved.getUserId());
        assertEquals(startTime, saved.getStartTime());
        assertEquals(20, saved.getDuration());
        assertEquals("music-1", saved.getMusicId());
        assertEquals(3L, saved.getImageId());
    }

    @Test
    void userDeleteMeditationLogShouldRejectOtherUsersLog() {
        MeditationLog existing = log(1L, 8L);
        when(meditationLogRepository.findById(1L)).thenReturn(Optional.of(existing));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> meditationService.userDeleteMeditationLog(7L, 1L));

        assertEquals("Unauthorized", exception.getMessage());
        verify(meditationLogRepository, never()).delete(any());
    }

    @Test
    void userDeleteMeditationLogShouldDeleteOwnLog() {
        MeditationLog existing = log(1L, 7L);
        when(meditationLogRepository.findById(1L)).thenReturn(Optional.of(existing));

        meditationService.userDeleteMeditationLog(7L, 1L);

        verify(meditationLogRepository).delete(existing);
    }

    @Test
    void startCountDownSessionShouldArchiveOldSessionAndPublishDelayMessage() {
        MeditationSession oldSession = session(11L, 7L, LocalDateTime.now().minusSeconds(30), 120);
        when(meditationSessionRepository.findByUserId(7L)).thenReturn(Optional.of(oldSession));
        when(meditationSessionRepository.save(any(MeditationSession.class))).thenAnswer(invocation -> {
            MeditationSession saved = invocation.getArgument(0);
            saved.setSessionId(12L);
            return saved;
        });
        MeditationCountDownStartRequest request = new MeditationCountDownStartRequest();
        request.setDuration(300);
        request.setMusicId(2L);
        request.setImageId(4L);

        MeditationSession newSession = meditationService.startCountDownSession(7L, request);

        assertEquals(7L, newSession.getUserId());
        assertEquals(300, newSession.getTargetDuration());
        assertEquals(2L, newSession.getMusicId());
        assertEquals(4L, newSession.getImageId());

        ArgumentCaptor<MeditationRecord> recordCaptor = ArgumentCaptor.forClass(MeditationRecord.class);
        verify(meditationRecordRepository).save(recordCaptor.capture());
        assertEquals("INTERRUPTED", recordCaptor.getValue().getStatus());
        verify(meditationSessionRepository).delete(oldSession);
        verify(rabbitTemplate).convertAndSend(eq("meditation.exchange"), eq("meditation.INTERRUPTED"), any(MeditationRecord.class));
        verify(rabbitTemplate).convertAndSend(eq("meditation.wait.exchange"), eq("wait.key"), eq(12L), any(MessagePostProcessor.class));
    }

    @Test
    void getSessionLeftDurationShouldReturnZeroWhenNoActiveSession() {
        when(meditationSessionRepository.findByUserId(7L)).thenReturn(Optional.empty());

        Integer leftDuration = meditationService.getSessionLeftDuration(7L);

        assertEquals(0, leftDuration);
    }

    @Test
    void getSessionLeftDurationShouldSubtractElapsedSeconds() {
        MeditationSession active = session(11L, 7L, LocalDateTime.now().minusSeconds(40), 120);
        when(meditationSessionRepository.findByUserId(7L)).thenReturn(Optional.of(active));

        Integer leftDuration = meditationService.getSessionLeftDuration(7L);

        assertEquals(80, leftDuration);
    }

    @Test
    void finishCountDownSessionShouldArchiveActiveSessionAsInterrupted() {
        MeditationSession active = session(11L, 7L, LocalDateTime.now().minusSeconds(20), 120);
        when(meditationSessionRepository.findByUserId(7L)).thenReturn(Optional.of(active));

        meditationService.finishCountDownSession(7L);

        ArgumentCaptor<MeditationRecord> recordCaptor = ArgumentCaptor.forClass(MeditationRecord.class);
        verify(meditationRecordRepository).save(recordCaptor.capture());
        assertEquals("INTERRUPTED", recordCaptor.getValue().getStatus());
        verify(meditationSessionRepository).delete(active);
    }

    @Test
    void handleSessionTimeoutShouldArchiveSessionAsCompleted() {
        MeditationSession active = session(11L, 7L, LocalDateTime.now().minusSeconds(200), 120);
        when(meditationSessionRepository.findById(11L)).thenReturn(Optional.of(active));

        meditationService.handleSessionTimeout(11L);

        ArgumentCaptor<MeditationRecord> recordCaptor = ArgumentCaptor.forClass(MeditationRecord.class);
        verify(meditationRecordRepository).save(recordCaptor.capture());
        assertEquals("COMPLETED", recordCaptor.getValue().getStatus());
        assertEquals(120, recordCaptor.getValue().getActualDuration());
        verify(meditationSessionRepository).delete(active);
        verify(rabbitTemplate).convertAndSend(eq("meditation.exchange"), eq("meditation.COMPLETED"), any(MeditationRecord.class));
    }

    @Test
    void handleUserDeleteShouldDeleteAllMeditationDataForUser() {
        meditationService.handleUserDelete(7L);

        verify(meditationLogRepository).deleteByUserId(7L);
        verify(meditationRecordRepository).deleteByUserId(7L);
        verify(meditationSessionRepository).deleteByUserId(7L);
    }

    private static MeditationLog log(Long id, Long userId) {
        MeditationLog log = new MeditationLog();
        log.setId(id);
        log.setUserId(userId);
        log.setStartTime(LocalDateTime.now());
        return log;
    }

    private static MeditationSession session(Long id, Long userId, LocalDateTime startTime, Integer targetDuration) {
        MeditationSession session = new MeditationSession();
        session.setSessionId(id);
        session.setUserId(userId);
        session.setStartTime(startTime);
        session.setTargetDuration(targetDuration);
        session.setMusicId(2L);
        session.setImageId(4L);
        return session;
    }
}
