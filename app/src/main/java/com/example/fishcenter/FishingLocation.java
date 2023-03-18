package com.example.fishcenter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

import java.lang.reflect.Array;
import java.util.ArrayList;

@Entity (tableName = "fishingLocation")
public class FishingLocation {
    @PrimaryKey(autoGenerate = true)
    public int key;
    @NonNull
    @ColumnInfo(name = "title")
    public String title;
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
    @ColumnInfo (name = "noOfRatings")
    public int noOfRatings;
    @Nullable
    @ColumnInfo (name = "userRating")
    public double userRating;
    @Nullable
    @ColumnInfo (name = "ratingsList")
    public ArrayList<Double> ratingsList;
    public FishingLocation(String title, String snippet, LatLng latLng, double overallRating, double userRating, ArrayList<Double> ratingsList){
        this.title = title;
        this.snippet = snippet;
        this.lat = latLng.latitude;
        this.lng = latLng.longitude;
        this.overallRating = overallRating;
        this.userRating = userRating;
        this.ratingsList = ratingsList;
    }

    public FishingLocation(String title, String snippet, double lat, double lng, double overallRating, double userRating, ArrayList<Double> ratingsList){
        this.title = title;
        this.snippet = snippet;
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
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getSnippet() {
        return snippet;
    }

    @Nullable
    public double getOverallRating() {return overallRating;}
    @Nullable
    public void setOverallRating(double overallRating) {
        this.overallRating = overallRating;
    }

    public void setRatingsList(ArrayList<Double> ratingsList) {
        this.ratingsList = ratingsList;
    }

    public ArrayList<Double> getRatingsList() {
        return ratingsList;
    }
    @Nullable
    public void setNoOfRatings(int noOfRatings) {
        this.noOfRatings = noOfRatings;
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
