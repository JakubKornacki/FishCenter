package com.example.fishcenter;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class OpenMeteoGetTidesData extends Thread {


    // OpenMeteo Marine Weather API allows to get realtime marine information
    // which is made available for use through the Open Meteo API thanks to
    // DWD Deutscher Wetterdienst Wetter und Klima aus einer Hand
    // https://open-meteo.com/en/docs/marine-weather-api
    // https://www.dwd.de/EN/service/copyright/copyright_node.html
    private Context con;
    private double lat;
    private double lng;
    private String response;
    private int responseCode;
    public OpenMeteoGetTidesData(Context con, double lat, double lng) {
        this.con = con;
        this.lat = lat;
        this.lng = lng;
    }

    @Override
    public void run() {
        super.run();
        try {
            URL url = new URL("https://marine-api.open-meteo.com/v1/marine?timezone=auto&latitude=" + lat + "&longitude=" + lng + "&hourly=wave_height");
            HttpURLConnection http = (HttpURLConnection) url.openConnection();

            BufferedReader responseDataReader = new BufferedReader(new InputStreamReader(http.getInputStream()));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = responseDataReader.readLine()) != null) {
                builder.append(line);
            }
            responseDataReader.close();
            if(http.getResponseCode() == 200) {
                response = parseJSONString(new JSONObject(builder.toString()));
            } else {
                response = null;
            }
            responseCode = http.getResponseCode();
            http.disconnect();
        } catch ( MalformedURLException e) {
            throw new RuntimeException(e);
        } catch ( IOException e) {
            // IOException is thrown whenever the user tries to place a marker on land don't throw it
            // since the behaviour of not placing markers on land is handled in the Map activity
        } catch ( JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private String parseJSONString(JSONObject jsonObject) {
        try {
            JSONArray time = jsonObject.getJSONObject("hourly").getJSONArray("time");
            JSONArray height = jsonObject.getJSONObject("hourly").getJSONArray("wave_height");
            String timezone = jsonObject.get("timezone").toString();
            String timezoneAbbreviation = jsonObject.get("timezone_abbreviation").toString();
            StringBuilder response = new StringBuilder();
            // decimal format to convert numbers like 4.9 to 4.90 so that the window displays consistently
            DecimalFormat df = new DecimalFormat("0.00");
            // from 00:00 to 24:00
            for (int i = 0; i <= 24; i+=3) {
                String[] timeSplit = time.getString(i).split("T");
                if(i == 0) {
                    response.append("(" + lat + "\t\t" + lng + ")\n");
                    response.append("Forecast for:\t" + timeSplit[0] + "\t" +  timezone + "\t" + timezoneAbbreviation + "\n");
                }
                if(timeSplit[1].equals("00:00")) {
                    timeSplit[1] = "24:00";
                }
                response.append("Time: " + timeSplit[1] + "\t\tHeight: " +  df.format(Double.valueOf(height.getString(i))) + " m\n");
            }
            return response.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public int getResponseCode(){
        return responseCode;
    }

    public String getTidesData() {
        return response;
    }
}
