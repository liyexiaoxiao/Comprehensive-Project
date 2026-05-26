package com.donffroodus.social_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.donffroodus.social_service.entity.CensorWord;
import com.donffroodus.social_service.repository.CensorWordRepository;

import jakarta.transaction.Transactional;
import java.util.regex.Pattern;

@Service
public class CensorService {
    @Autowired
    private CensorWordRepository censorWordRepository;

    public List<CensorWord> getAllCensorWords(int active) {
        if (active == 1) {
            return censorWordRepository.findByIsActiveTrue();
        } else {
            return censorWordRepository.findAll();
        }
    }

    public CensorWord getCensorWordByWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            throw new RuntimeException("Word cannot be empty");
        }
        CensorWord censorWord = censorWordRepository.findByWord(word)
                .orElseThrow(() -> new RuntimeException("Censor word not found"));
        return censorWord;
    }

    public void addCensorWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            throw new RuntimeException("Word cannot be empty");
        }
        if (censorWordRepository.existsByWord(word)) {
            throw new RuntimeException("Censor word already exists");
        }
        CensorWord newWord = new CensorWord();
        newWord.setWord(word);
        newWord.setActive(true);
        newWord.setCreatedAt(java.time.LocalDateTime.now());
        censorWordRepository.save(newWord);
    }

    public void setWordActivity(String word, boolean active) {
        if (word == null || word.trim().isEmpty()) {
            throw new RuntimeException("Word cannot be empty");
        }
        CensorWord censorWord = censorWordRepository.findByWord(word)
                .orElseThrow(() -> new RuntimeException("Censor word not found"));
        censorWord.setActive(active);
        censorWordRepository.save(censorWord);
    }

    @Transactional
    public void deleteCensorWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            throw new RuntimeException("Word cannot be empty");
        }
        CensorWord censorWord = censorWordRepository.findByWord(word)
                .orElseThrow(() -> new RuntimeException("Censor word not found"));
        censorWordRepository.delete(censorWord);
    }

    public String CensorContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        String wordPattern = null;
        List<CensorWord> activeCensorWords = censorWordRepository.findByIsActiveTrue();
        if (activeCensorWords.isEmpty()) {
            return content;
        }
        wordPattern = activeCensorWords.stream()
                .map(CensorWord::getWord)
                .filter(word -> word != null && !word.trim().isEmpty())
                .reduce((w1, w2) -> w1 + "|" + w2)
                .orElse("");
        Pattern pattern = Pattern.compile("(" + wordPattern + ")", Pattern.CASE_INSENSITIVE);

        return pattern.matcher(content).replaceAll(match -> "*".repeat(match.group().length()));
    }
}
