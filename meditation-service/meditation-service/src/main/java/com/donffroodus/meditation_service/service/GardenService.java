package com.donffroodus.meditation_service.service;

import com.donffroodus.meditation_service.dto.GardenDataResponse;
import com.donffroodus.meditation_service.dto.GardenItemDTO;
import com.donffroodus.meditation_service.entity.GardenInventory;
import com.donffroodus.meditation_service.entity.UnlockedPlant;
import com.donffroodus.meditation_service.repository.GardenInventoryRepository;
import com.donffroodus.meditation_service.repository.UnlockedPlantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class GardenService {

    private final GardenInventoryRepository inventoryRepository;
    private final UnlockedPlantRepository plantRepository;
    private final Random random = new Random();

    public GardenService(GardenInventoryRepository inventoryRepository, UnlockedPlantRepository plantRepository) {
        this.inventoryRepository = inventoryRepository;
        this.plantRepository = plantRepository;
    }

    public GardenDataResponse getUserGardenData(Long userId) {
        List<GardenInventory> inventory = inventoryRepository.findByUserId(userId);
        
        List<GardenItemDTO> seeds = inventory.stream()
                .filter(i -> "SEED".equals(i.getItemType()))
                .map(i -> new GardenItemDTO(i.getItemId(), i.getCount()))
                .collect(Collectors.toList());
                
        List<GardenItemDTO> fruits = inventory.stream()
                .filter(i -> "FRUIT".equals(i.getItemType()))
                .map(i -> new GardenItemDTO(i.getItemId(), i.getCount()))
                .collect(Collectors.toList());
                
        List<Integer> unlockedPlantIds = plantRepository.findByUserId(userId).stream()
                .map(UnlockedPlant::getPlantId)
                .collect(Collectors.toList());
                
        return new GardenDataResponse(seeds, fruits, unlockedPlantIds);
    }

    @Transactional
    public GardenItemDTO rewardRandomItem(Long userId) {
        // Randomly reward a seed (ID 1-3) or fruit (ID 1-2)
        boolean isSeed = random.nextDouble() > 0.3; // 70% chance for seed
        String type = isSeed ? "SEED" : "FRUIT";
        int itemId = isSeed ? (random.nextInt(3) + 1) : (random.nextInt(2) + 1);
        
        GardenInventory item = inventoryRepository.findByUserIdAndItemTypeAndItemId(userId, type, itemId)
                .orElseGet(() -> {
                    GardenInventory newItem = new GardenInventory();
                    newItem.setUserId(userId);
                    newItem.setItemType(type);
                    newItem.setItemId(itemId);
                    newItem.setCount(0);
                    return newItem;
                });
                
        item.setCount(item.getCount() + 1);
        inventoryRepository.save(item);
        
        return new GardenItemDTO(itemId, 1);
    }
    
    @Transactional
    public boolean unlockPlant(Long userId, Integer plantId) {
        if (plantRepository.existsByUserIdAndPlantId(userId, plantId)) {
            return false; // Already unlocked
        }
        
        UnlockedPlant plant = new UnlockedPlant();
        plant.setUserId(userId);
        plant.setPlantId(plantId);
        plantRepository.save(plant);
        return true;
    }
}
