package com.donffroodus.social_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donffroodus.social_service.entity.CensorWord;

public interface CensorWordRepository extends JpaRepository<CensorWord, Long> {
    List<CensorWord> findByIsActiveTrue();
    
}
