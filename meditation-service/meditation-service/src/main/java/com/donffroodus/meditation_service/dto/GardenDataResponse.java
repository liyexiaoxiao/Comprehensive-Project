package com.donffroodus.meditation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GardenDataResponse {
    private List<GardenItemDTO> seeds;
    private List<GardenItemDTO> fruits;
    private List<Integer> unlockedPlantIds;
}
