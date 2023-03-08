package com.example.fishcenter;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

@Dao
public interface FishingLocationDao {
    @Query("SELECT * FROM FishingLocation")
    List<FishingLocation> getAllFishingLocations();
    @Insert
    void addFishingLocation(FishingLocation fishingLocation);
    @Update
    void updateFishingLocation(FishingLocation fishingLocation);
    @Delete
    void deleteFishingLocation(FishingLocation fishingLocation);

    @Query("SELECT * FROM FishingLocation WHERE lat  == :latToCheck AND lng == :lngToCheck")
    FishingLocation findWeatherStationByLatLng(double latToCheck, double lngToCheck);
}
