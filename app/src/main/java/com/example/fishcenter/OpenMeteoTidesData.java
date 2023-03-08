package com.example.fishcenter;

import android.content.Context;

import com.google.firebase.Timestamp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;

public class OpenMeteoTidesData extends Thread {

    private Context con;
    private double lat;
    private double lng;
    private String response;

    public OpenMeteoTidesData(Context con, double lat, double lng) {
        this.con = con;
        this.lat = lat;
        this.lng = lng;
    }

    @Override
    public void run() {
        super.run();
        try {
            // https://marine-api.open-meteo.com/v1/marine?latitude=-15.9167&lon=-5.7&key=f97143bf-02ec-4637-8723-53286ffd5860
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-mm-dd");
            String dateToday = dateFormatter.format(Timestamp.now().toDate());
            String key = con.getString(R.string.WORLDTIDES_API_KEY);
            URL url = new URL("https://marine-api.open-meteo.com/v1/marine?latitude=" + lat + "&longitude=" + lng + "&hourly=wave_height");
            HttpURLConnection http = (HttpURLConnection) url.openConnection();

            BufferedReader responseDataReader = new BufferedReader(new InputStreamReader(http.getInputStream()));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = responseDataReader.readLine()) != null) {
                builder.append(line);
            }
            responseDataReader.close();
            parseJSONString(new JSONObject(builder.toString()), http.getResponseCode());
            http.disconnect();
        } catch ( MalformedURLException e) {
            throw new RuntimeException(e);
        } catch ( IOException e) {
            // don't throw the exception but set the response to below this happens when the user places a marker on land
            response = "No data is available for this location!";
        } catch ( JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void parseJSONString(JSONObject jsonObject, int responseCode) {
        if(responseCode == 200) {
            try {
                JSONArray time = jsonObject.getJSONObject("hourly").getJSONArray("time");
                JSONArray height = jsonObject.getJSONObject("hourly").getJSONArray("wave_height");
                for (int i = 0; i < 24; i += 6) {
                    response += time.getString(i) + "\t" + height.getString(i) + "\n";
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getTidesData() {
        return response;
    }
}
