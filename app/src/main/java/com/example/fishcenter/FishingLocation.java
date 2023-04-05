package com.example.fishcenter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

@Entity (tableName = "fishingLocation")
public class FishingLocation {
    @PrimaryKey(autoGenerate = true)
    public int key;
    @NonNull
    @ColumnInfo(name = "title")
    public String locationName;
    @Nullable
    @ColumnInfo (name = "snippet")
    public String snippet;
    @NonNull
    @ColumnInfo (name = "lat")
    public double lat;
    @NonNull
    @ColumnInfo (name = "lng")
    public double lng;
    @Nullable
    @ColumnInfo (name = "overallRating")
    public double overallRating;
    @Nullable
    @ColumnInfo (name = "userRating")
    public double userRating;
    @Nullable
    @ColumnInfo (name = "ratingsList")
    public ArrayList<Double> ratingsList;
    public FishingLocation(String locationName, LatLng latLng, double overallRating, double userRating, ArrayList<Double> ratingsList){
        this.locationName = locationName;
        this.lat = latLng.latitude;
        this.lng = latLng.longitude;
        this.overallRating = overallRating;
        this.userRating = userRating;
        this.ratingsList = ratingsList;
    }

    public FishingLocation(String locationName, double lat, double lng, double overallRating, double userRating, ArrayList<Double> ratingsList){
        this.locationName = locationName;
        this.lat = lat;
        this.lng = lng;
        this.overallRating = overallRating;
        this.userRating = userRating;
        this.ratingsList = ratingsList;
    }
    @NonNull
    public LatLng getPosition() {
        return new LatLng(lat, lng);
    }

    @Nullable
    public String getLocationName() {
        return locationName;
    }

    @Nullable
    public double getOverallRating() {return overallRating;}
    @Nullable
    public void setOverallRating(double overallRating) {
        this.overallRating = overallRating;
    }
    @Nullable
    public void setRatingsList(ArrayList<Double> ratingsList) {
        this.ratingsList = ratingsList;
    }
    @Nullable
    public ArrayList<Double> getRatingsList() {
        return ratingsList;
    }

    @Nullable
    public void setUserRating(double userRating) {
        this.userRating = userRating;
    }
    @Nullable
    public double getUserRating() {
        return userRating;
    }

}
