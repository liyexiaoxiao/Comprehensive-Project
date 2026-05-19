package com.donffroodus.meditation_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.donffroodus.meditation_service.dto.GardenDataResponse;
import com.donffroodus.meditation_service.dto.GardenItemDTO;
import com.donffroodus.meditation_service.entity.GardenInventory;
import com.donffroodus.meditation_service.entity.UnlockedPlant;
import com.donffroodus.meditation_service.repository.GardenInventoryRepository;
import com.donffroodus.meditation_service.repository.UnlockedPlantRepository;

@ExtendWith(MockitoExtension.class)
class GardenServiceTest {

    @Mock
    private GardenInventoryRepository inventoryRepository;

    @Mock
    private UnlockedPlantRepository plantRepository;

    @Test
    void getUserGardenDataShouldSplitSeedsFruitsAndUnlockedPlants() {
        GardenService service = new GardenService(inventoryRepository, plantRepository);
        when(inventoryRepository.findByUserId(7L)).thenReturn(List.of(
                inventory(7L, "SEED", 1, 3),
                inventory(7L, "FRUIT", 2, 5)));
        when(plantRepository.findByUserId(7L)).thenReturn(List.of(plant(7L, 4), plant(7L, 6)));

        GardenDataResponse response = service.getUserGardenData(7L);

        assertEquals(List.of(1), response.getSeeds().stream().map(GardenItemDTO::getItemId).toList());
        assertEquals(List.of(3), response.getSeeds().stream().map(GardenItemDTO::getCount).toList());
        assertEquals(List.of(2), response.getFruits().stream().map(GardenItemDTO::getItemId).toList());
        assertEquals(List.of(5), response.getFruits().stream().map(GardenItemDTO::getCount).toList());
        assertEquals(List.of(4, 6), response.getUnlockedPlantIds());
    }

    @Test
    void rewardRandomItemShouldIncrementExistingInventoryItem() {
        GardenService service = new GardenService(inventoryRepository, plantRepository);
        GardenInventory existing = inventory(7L, "SEED", 1, 2);
        when(inventoryRepository.findByUserIdAndItemTypeAndItemId(any(), any(), any()))
                .thenReturn(Optional.of(existing));

        GardenItemDTO reward = service.rewardRandomItem(7L);

        assertEquals(1, reward.getCount());
        assertEquals(3, existing.getCount());
        verify(inventoryRepository).save(existing);
    }

    @Test
    void rewardRandomItemShouldCreateInventoryItemWhenMissing() {
        GardenService service = new GardenService(inventoryRepository, plantRepository);
        when(inventoryRepository.findByUserIdAndItemTypeAndItemId(any(), any(), any()))
                .thenReturn(Optional.empty());

        GardenItemDTO reward = service.rewardRandomItem(7L);

        ArgumentCaptor<GardenInventory> inventoryCaptor = ArgumentCaptor.forClass(GardenInventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());
        assertEquals(7L, inventoryCaptor.getValue().getUserId());
        assertTrue(List.of("SEED", "FRUIT").contains(inventoryCaptor.getValue().getItemType()));
        assertEquals(1, inventoryCaptor.getValue().getCount());
        assertEquals(inventoryCaptor.getValue().getItemId(), reward.getItemId());
    }

    @Test
    void unlockPlantShouldReturnFalseWhenPlantAlreadyUnlocked() {
        GardenService service = new GardenService(inventoryRepository, plantRepository);
        when(plantRepository.existsByUserIdAndPlantId(7L, 3)).thenReturn(true);

        boolean success = service.unlockPlant(7L, 3);

        assertFalse(success);
    }

    @Test
    void unlockPlantShouldSaveNewPlantWhenNotUnlocked() {
        GardenService service = new GardenService(inventoryRepository, plantRepository);
        when(plantRepository.existsByUserIdAndPlantId(7L, 3)).thenReturn(false);

        boolean success = service.unlockPlant(7L, 3);

        assertTrue(success);
        ArgumentCaptor<UnlockedPlant> plantCaptor = ArgumentCaptor.forClass(UnlockedPlant.class);
        verify(plantRepository).save(plantCaptor.capture());
        assertEquals(7L, plantCaptor.getValue().getUserId());
        assertEquals(3, plantCaptor.getValue().getPlantId());
    }

    private static GardenInventory inventory(Long userId, String type, Integer itemId, Integer count) {
        GardenInventory inventory = new GardenInventory();
        inventory.setUserId(userId);
        inventory.setItemType(type);
        inventory.setItemId(itemId);
        inventory.setCount(count);
        return inventory;
    }

    private static UnlockedPlant plant(Long userId, Integer plantId) {
        UnlockedPlant plant = new UnlockedPlant();
        plant.setUserId(userId);
        plant.setPlantId(plantId);
        return plant;
    }
}
