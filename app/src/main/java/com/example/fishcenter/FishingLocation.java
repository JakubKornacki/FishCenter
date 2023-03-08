package com.example.fishcenter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

@Entity (tableName = "fishingLocation")
public class FishingLocation implements ClusterItem {
    @PrimaryKey(autoGenerate = true)
    public int key;
    @NonNull
    @ColumnInfo(name = "title")
    public String title;
    @ColumnInfo (name = "snippet")
    public String snippet;
    @NonNull
    @ColumnInfo (name = "lat")
    public double lat;
    @NonNull
    @ColumnInfo (name = "lng")
    public double lng;

    public FishingLocation(String title, String snippet, LatLng latLng){
        this.title = title;
        this.snippet = snippet;
        this.lat = latLng.latitude;
        this.lng = latLng.longitude;
    }

    // Constructor with parameters
    public FishingLocation(String title, String snippet, double lat, double lng){
        this.title = title;
        this.snippet = snippet;
        this.lat = lat;
        this.lng = lng;
    }
    @NonNull
    @Override
    public LatLng getPosition() {
        return new LatLng(lat, lng);
    }

    @Nullable
    @Override
    public String getTitle() {
        return title;
    }

    @Nullable
    @Override
    public String getSnippet() {
        return snippet;
    }
}
