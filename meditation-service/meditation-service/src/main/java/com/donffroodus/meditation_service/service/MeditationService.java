package com.donffroodus.meditation_service.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donffroodus.meditation_service.dto.MeditationCountDownStartRequest;
import com.donffroodus.meditation_service.dto.MeditationRequest;
import com.donffroodus.meditation_service.entity.MeditationLog;
import com.donffroodus.meditation_service.entity.MeditationRecord;
import com.donffroodus.meditation_service.entity.MeditationSession;
import com.donffroodus.meditation_service.repository.MeditationLogRepository;
import com.donffroodus.meditation_service.repository.MeditationRecordRepository;
import com.donffroodus.meditation_service.repository.MeditationSessionRepository;

@Service
public class MeditationService {
    @Autowired
    private MeditationLogRepository meditationLogRepository;

    @Autowired
    private MeditationRecordRepository meditationRecordRepository;

    @Autowired
    private MeditationSessionRepository meditationSessionRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public List<MeditationLog> getMediListByUserId(Long userId) {
        return meditationLogRepository.findByUserIdOrderByStartTimeDesc(userId);
    }

    public MeditationLog saveMeditationLog(Long userId, MeditationRequest request) {
        MeditationLog log = new MeditationLog();
        log.setUserId(userId);
        LocalDateTime startTime = request.getStartTime() != null ? request.getStartTime() : LocalDateTime.now();
        log.setStartTime(startTime);
        log.setDuration(request.getDuration());
        log.setMusicId(request.getMusicId());
        log.setImageId(request.getImageId());
        return meditationLogRepository.save(log);
    }

    public void userDeleteMeditationLog(Long userId, Long logId) {
        MeditationLog log = meditationLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Meditation log not found"));
        if (!log.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        meditationLogRepository.delete(log);
    }

    public void adminDeleteMeditationLog(Long logId) {
        MeditationLog log = meditationLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Meditation log not found"));
        meditationLogRepository.delete(log);
    }

    @Transactional
    public MeditationSession startCountDownSession(Long userId, MeditationCountDownStartRequest request) {
        meditationSessionRepository.findByUserId(userId).ifPresent(oldSession -> {
            archiveSession(oldSession, "INTERRUPTED");
        });

        MeditationSession newSession = new MeditationSession();
        newSession.setUserId(userId);
        newSession.setStartTime(LocalDateTime.now());
        newSession.setTargetDuration(request.getDuration());
        newSession.setMusicId(request.getMusicId());
        newSession.setImageId(request.getImageId());
        meditationSessionRepository.save(newSession);

        rabbitTemplate.convertAndSend(
            "meditation.wait.exchange", 
            "wait.key", 
            newSession.getSessionId(),
            message -> {
                message.getMessageProperties().setExpiration(String.valueOf(request.getDuration() * 1000));
                return message;
            }
        );

        return newSession;
    }

    @Transactional
    public void finishCountDownSession(Long userId) {
        meditationSessionRepository.findByUserId(userId).ifPresent(session -> {
            archiveSession(session, "INTERRUPTED");
        });
    }

    private void archiveSession(MeditationSession session, String status) {
        LocalDateTime endTime = LocalDateTime.now();
        long duration = ChronoUnit.SECONDS.between(session.getStartTime(), endTime);

        MeditationRecord record = new MeditationRecord();
        record.setUserId(session.getUserId());
        record.setStartTime(session.getStartTime());
        record.setActualDuration(Math.min((int) duration, session.getTargetDuration()));
        record.setMusicId(session.getMusicId());
        record.setImageId(session.getImageId());
        record.setStatus(status);

        meditationRecordRepository.save(record);
        meditationSessionRepository.delete(session);

        String routingKey = "meditation." + status;
        rabbitTemplate.convertAndSend("meditation.exchange", routingKey, record);
    }

    @RabbitListener(bindings=@QueueBinding(
        value = @Queue(value = "meditation.timeout.dlq", durable = "true"),
        exchange = @Exchange(value = "meditation.timeout.dlx", type = "topic"),
        key = "timeout.key"
    ))
    @Transactional
    public void handleSessionTimeout(Long sessionId) {
        meditationSessionRepository.findById(sessionId).ifPresent(session -> {
            archiveSession(session, "COMPLETED");
        });
    }

    @RabbitListener(bindings=@QueueBinding(
        value = @Queue(value = "meditation.user.delete.queue"),
        exchange = @Exchange(value = "user.exchange", type = "topic"),
        key = "user.delete"
    ))
    public void handleUserDelete(Long id) {
        System.out.println("Received user delete message: " + id);
        meditationLogRepository.deleteByUserId(id);
        meditationRecordRepository.deleteByUserId(id);
        meditationSessionRepository.deleteByUserId(id);
    }
}
