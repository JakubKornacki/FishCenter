package com.example.fishcenter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class Fish implements Serializable {
    private String latinName;
    private float predictionAccuracy;
    private String fishName;
    private String mediaUri;
    private String[] commonNames;
    private String distribution;
    private boolean scales;
    private boolean saltWater;
    private boolean freshWater;
    private String coloration;
    private String feedingBehaviour;
    private String healthWarnings;
    private String foodValue;
    private String[] similarSpecies;
    private String environmentDetail;

    public Fish (String latinName, float predictionAccuracy, String fishName, String mediaUri, String[] commonNames, String distribution, boolean scales, boolean saltWater, boolean freshWater, String coloration, String feedingBehaviour, String healthWarnings, String foodValue, String[] similarSpecies, String environmentDetail) {
        this.latinName = latinName;
        this.predictionAccuracy = predictionAccuracy;
        this.fishName = fishName;
        this.mediaUri = mediaUri;
        this.commonNames = commonNames;
        this.distribution = distribution;
        this.scales = scales;
        this.saltWater = saltWater;
        this.freshWater = freshWater;
        this.coloration = coloration;
        this.feedingBehaviour = feedingBehaviour;
        this.healthWarnings = healthWarnings;
        this.foodValue = foodValue;
        this.similarSpecies = similarSpecies;
        this.environmentDetail = environmentDetail;
    }


    public String getLatinName() {
        return latinName;
    }

    public float getPredictionAccuracy() {
        return predictionAccuracy;
    }

    public String getFishName() {
        return fishName;
    }

    public String getMediaUri() {
        return mediaUri;
    }

    public String[] getCommonNames() {
        return commonNames;
    }

    public String getDistribution() {
        return distribution;
    }

    public boolean hasScales() {
        return scales;
    }

    public boolean isSaltWater() {
        return saltWater;
    }

    public boolean isFreshWater() {
        return freshWater;
    }

    public String getColoration() {
        return coloration;
    }

    public String getFeedingBehaviour() {
        return feedingBehaviour;
    }

    public String getHealthWarnings() {
        return healthWarnings;
    }

    public String getFoodValue() {
        return foodValue;
    }

    public String[] getSimilarSpecies() {
        return similarSpecies;
    }

    public String getEnvironmentDetail() {
        return environmentDetail;
    }

    // hashcode and equals methods need to be Overridden to ensure that the hashset can differentiate between two objects
    // auto generated equals method
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Fish fish = (Fish) o;
        return Float.compare(fish.predictionAccuracy, predictionAccuracy) == 0 && scales == fish.scales && saltWater == fish.saltWater && freshWater == fish.freshWater && Objects.equals(latinName, fish.latinName) && Objects.equals(fishName, fish.fishName) && Objects.equals(mediaUri, fish.mediaUri) && Arrays.equals(commonNames, fish.commonNames) && Objects.equals(distribution, fish.distribution) && Objects.equals(coloration, fish.coloration) && Objects.equals(feedingBehaviour, fish.feedingBehaviour) && Objects.equals(healthWarnings, fish.healthWarnings) && Objects.equals(foodValue, fish.foodValue) && Arrays.equals(similarSpecies, fish.similarSpecies) && Objects.equals(environmentDetail, fish.environmentDetail);
    }

    // auto generated hash code method
    @Override
    public int hashCode() {
        int result = Objects.hash(latinName, predictionAccuracy, fishName, mediaUri, distribution, scales, saltWater, freshWater, coloration, feedingBehaviour, healthWarnings, foodValue, environmentDetail);
        result = 31 * result + Arrays.hashCode(commonNames);
        result = 31 * result + Arrays.hashCode(similarSpecies);
        return result;
    }
}
