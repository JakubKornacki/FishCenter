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
    private Context con;
    private double lat;
    private double lng;
    private String response = "";
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
            responseCode = http.getResponseCode();
            parseJSONString(new JSONObject(builder.toString()), responseCode);
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

    private void parseJSONString(JSONObject jsonObject, int responseCode) {
        if(responseCode == 200) {
            try {
                JSONArray time = jsonObject.getJSONObject("hourly").getJSONArray("time");
                JSONArray height = jsonObject.getJSONObject("hourly").getJSONArray("wave_height");
                String timezone = jsonObject.get("timezone").toString();
                String timezoneAbbreviation = jsonObject.get("timezone_abbreviation").toString();
                // decimal format to convert numbers like 4.9 to 4.90 so that the window displays consistently
                DecimalFormat df = new DecimalFormat("0.00");
                // tab does not seem to work when the string is added to the textview with .setText() so 8 spaces are hardcoded here to give the same effect of two tabs (\t\t)
                // from 00:00 to 24:00
                for (int i = 0; i <= 24; i+=3) {
                    String[] timeSplit = time.getString(i).split("T");
                    if(i == 0) {
                        response += "(" + lat + "    " + lng + ")\n";
                        response += "Forecast for:    " + timeSplit[0] + "    " +  timezone + "    " + timezoneAbbreviation + "\n";
                    }
                    if(timeSplit[1].equals("00:00")) {
                        timeSplit[1] = "24:00";
                    }
                    response += "Time: " + timeSplit[1] + "        Height: " +  df.format(Double.valueOf(height.getString(i))) + " m\n";
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public int getResponseCode() {
        return responseCode;
    }

    public String getTidesData() {
        return response;
    }
}
