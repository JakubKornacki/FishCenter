package com.example.fishcenter;

import java.io.Serializable;

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

}
