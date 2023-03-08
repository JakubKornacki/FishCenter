package com.example.fishcenter;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class OneOffSaveOfWeatherStationsToFirestore extends Thread {
    private Context con;
    private final FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();

    public OneOffSaveOfWeatherStationsToFirestore(Context con) {
        this.con = con;
    }


    @Override
    public void run() {
        super.run();
        try {
            String APIKEY = con.getString(R.string.WORLDTIDES_API_KEY);
            // get all station within the radius of 15,000km (to be sure that all are fetched) away from the default location
            URL url = new URL("https://www.worldtides.info/api/v3?stations&lat=33.768321&lon=-118.195617&stationDistance=150000&key=" + APIKEY);
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            if(http.getResponseCode() == 200) {
                Log.i(TAG,"Weather stations fetched");
                BufferedReader responseDataReader = new BufferedReader(new InputStreamReader(http.getInputStream()));
                // StringBuilder to build the response
                StringBuilder httpResponse = new StringBuilder();
                // go through the token data line by line and append each line to the StringBuilder
                String line;
                while ((line = responseDataReader.readLine()) != null) {
                    httpResponse.append(line);
                }
                // close the buffered reader
                responseDataReader.close();
                // get fish data in an JSONObject
                JSONObject response = new JSONObject(httpResponse.toString());
                for(int i = 0; i < response.getJSONArray("stations").length(); i++) {
                    JSONObject weatherStation = response.getJSONArray("stations").getJSONObject(i);
                    Map<String, Object> weatherStationMap = new HashMap<>();
                    weatherStationMap.put("id", weatherStation.get("id"));
                    weatherStationMap.put("name", weatherStation.get("name"));
                    weatherStationMap.put("lat", weatherStation.get("lat"));
                    weatherStationMap.put("lon", weatherStation.get("lon"));
                    weatherStationMap.put("timezone", weatherStation.get("timezone"));
                    firebaseFirestore.collection("weatherStations").document(weatherStation.getString("id")).set(weatherStationMap);
                }
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }
}
