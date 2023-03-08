package com.example.fishcenter;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class CreateUserWeatherStationDatabase extends Thread {

    private static boolean isFinished;
    private Context con;
    private FishingLocationDatabase fishingLocationDatabase;
    public CreateUserWeatherStationDatabase(Context con, FishingLocationDatabase fishingLocationDatabase) {
        this.con = con;
        this.fishingLocationDatabase = fishingLocationDatabase;
    }

    @Override
    public void run() {
        super.run();
        try {
            BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(con.getAssets().open("WeatherStations.txt")));
            String line;
            StringBuilder builder = new StringBuilder();
            while((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }
            // close the buffered read
            bufferedReader.close();
            // get fish data in an JSONObject
            JSONObject response = new JSONObject(builder.toString());
            FishingLocationDao fishingLocationDao = fishingLocationDatabase.fishingLocationDao();
            for(int i = 0; i < response.getJSONArray("stations").length(); i++) {
                JSONObject weatherStation = response.getJSONArray("stations").getJSONObject(i);
                LatLng position = new LatLng(Double.valueOf((String) weatherStation.get("lat")), Double.valueOf((String) weatherStation.get("lon")));
                FishingLocation temp = new FishingLocation(
                    String.valueOf(weatherStation.get("id")),
                    String.valueOf(weatherStation.get("name")),
                    position
                );
                fishingLocationDao.addFishingLocation(temp);
            }
            isFinished = true;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isFinished() {
        return isFinished;
    }
    // to check if a database exists for a user it can be queries with exists()
    // method since the database is stored as a file on the user's device
    // https://stackoverflow.com/a/12025733
    public static boolean databaseExists(Context con) {
        File databaseFile = con.getDatabasePath("weathestationdatabase");
        if(databaseFile.exists()) {
            isFinished = true;
            return true;
        }
        return false;
    }
}
