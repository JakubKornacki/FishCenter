package com.example.fishcenter;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public interface FishingLocationsCallback {
    void currentFishingLocationReady(FishingLocation currentFishingLocation);
    void fishingLocationUpdated(LatLng position);
    void fishingLocationRatingUpdated(boolean previouslyUpdated, double newUserRating);
    void fishingLocationsReady(ArrayList<FishingLocation> fishingLocations);
    void isSynchronisationNecessary(boolean isNecessary);
}
