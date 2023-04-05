package com.example.fishcenter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CustomFishingLocationInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
    private Context context;
    public String name;
    public String data;

    public CustomFishingLocationInfoWindowAdapter(Context context) {
        this.context = context;
    }
    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        View customInfoWindow = LayoutInflater.from(context).inflate(R.layout.layout_fishing_location, null);
        TextView locationName = customInfoWindow.findViewById(R.id.fishingLocationName);
        TextView locationData = customInfoWindow.findViewById(R.id.data);

        if(name != null) {
            locationName.setText(name);
        } else {
            locationName.setText("Location with no name!\n");
        }
        if(data != null) {
            locationData.setText(data);
        }
        // if return null then contents of getInfoWindow are used to display the info window
        // view will be framed in standard google maps info window and will only replace the inner contents
        return customInfoWindow;
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        return null;
    }

    public void setLocationName(String name) {
        this.name = name;
    }

    public void setLocationData(String data) {
        this.data = data;
    }

}
