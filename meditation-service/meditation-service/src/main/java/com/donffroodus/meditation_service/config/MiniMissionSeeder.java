package com.donffroodus.meditation_service.config;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.donffroodus.meditation_service.entity.MiniMission;
import com.donffroodus.meditation_service.repository.MiniMissionRepository;

@Configuration
public class MiniMissionSeeder {

    @Bean
    CommandLineRunner seedDefaultMiniMissions(MiniMissionRepository miniMissionRepository) {
        return args -> {
            List<MiniMissionSeed> defaults = List.of(
                new MiniMissionSeed("晒太阳 5 分钟", "到窗边/阳台/户外，感受光线与温度，什么都不需要想。", 1),
                new MiniMissionSeed("喝一杯水", "立刻让身体完成一次小任务，给大脑一个可完成的证据。", 1),
                new MiniMissionSeed("整理桌面 2 分钟", "只收拾一个角落，不要试图整理整个房间。", 1),
                new MiniMissionSeed("给朋友发一句消息", "一句就好，不用展开聊，恢复一点连接感。", 1),
                new MiniMissionSeed("暴露阶梯：做“第一小步”", "把焦虑情境拆成阶梯，今天只做最小的一步。", 2),
                new MiniMissionSeed("延期担忧：把担心推迟到固定时间", "给担忧设置时间边界，让当下先回到可执行的小事。", 2)
            );

            for (MiniMissionSeed seed : defaults) {
                MiniMission mission = miniMissionRepository.findByTitle(seed.title())
                    .orElseGet(MiniMission::new);

                if (mission.getCreatedAt() == null) {
                    mission.setCreatedAt(LocalDateTime.now());
                }
                mission.setTitle(seed.title());
                mission.setDescription(seed.description());
                mission.setRewardValue(seed.rewardValue());
                mission.setActive(true);
                mission.setUpdatedAt(LocalDateTime.now());
                miniMissionRepository.save(mission);
            }
        };
    }

    private record MiniMissionSeed(String title, String description, int rewardValue) {}
}
