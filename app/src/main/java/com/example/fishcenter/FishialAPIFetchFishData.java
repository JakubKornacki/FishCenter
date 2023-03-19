package com.example.fishcenter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;

public class FishialAPIFetchFishData extends Thread {
    // get the access token to use the Fishial.AI api
    private JSONObject accessToken;
    private JSONObject dataForCloudImageUpload;
    private JSONObject fishialImageRecognitionData;
    private FishImage fishImage;
    private Context context;


    public FishialAPIFetchFishData(FishImage fishImage, Context context) {
        this.fishImage = fishImage;
        this.context = context;
    }

    @Override
    public void run() {
        try {
            super.run();
            accessToken = fetchAuthorizationToken();
            dataForCloudImageUpload = obtainDataForCloudUpload(accessToken, fishImage);
            uploadAnImageToTheCloud(dataForCloudImageUpload, fishImage);
            fishialImageRecognitionData = fishDetection(dataForCloudImageUpload, accessToken);
            // get Activity from context
            // https://stackoverflow.com/questions/9891360/getting-activity-from-context-in-android
            Activity activity = (Activity) context;
            // way of passing objects between activities using Intent with the help of serializable interface
            // https://stackoverflow.com/questions/13601883/how-to-pass-arraylist-of-objects-from-one-to-another-activity-using-intent-in-an
            // ArrayList of Fish objects is created by parsing out the JSON data
            if(fishialImageRecognitionData.getJSONArray("results").length() != 0) {
                Intent fishRecognisedIntent = new Intent(activity, FishRecognisedActivity.class);
                HashSet<Fish> fishes = parseJSONToFishObjectArrayList(fishialImageRecognitionData);
                // New bundle which will be attached to the intent and passed over to the FishRecognisedActivity class
                Bundle bundle = new Bundle();
                // since Fish class is serializable we can put in the ArrayList of Fish object into the bundle and give it a key "fishes"
                bundle.putSerializable("fishes", fishes);
                // attach the Bundle object holding the ArrayList of Fish objects to the Intent
                fishRecognisedIntent.putExtra("bundle", bundle);
                // Start the activity called FishRecognisedActivity, the bundle with Fish objects should be attached and transferred over
                context.startActivity(fishRecognisedIntent);
            } else {
                Intent fishNotRecognisedOrOutOfAPICredits = new Intent(activity, FishNotRecognisedOrOutOfAPICredits.class);
                context.startActivity(fishNotRecognisedOrOutOfAPICredits);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    private HashSet<Fish> parseJSONToFishObjectArrayList(JSONObject fishData){
        try {
            HashSet<Fish> fishes = new HashSet<>();
            // get the species arrayList from fishData (there is one entry for each fish identified in an image)
            for(int i = 0; i < fishData.getJSONArray("results").length(); i++) {
                JSONObject result = fishData.getJSONArray("results").getJSONObject(i);
                JSONArray species = result.getJSONArray("species");
                for (int j = 0; j < species.length(); j++) {
                    // always first object
                    JSONObject fishEntryInJSON = species.getJSONObject(0);

                    // get fish description as variables
                    String latinName = (fishEntryInJSON.getString("name") != null) ? fishEntryInJSON.getString("name") : "Unknown";
                    float accuracy = (fishEntryInJSON.getString("accuracy") != null) ? Float.parseFloat(fishEntryInJSON.getString("accuracy")) : null;

                    JSONObject fishAnglerData = fishEntryInJSON.getJSONObject("fishangler-data");
                    String title = (fishAnglerData.has("title")) ? fishAnglerData.getString("title") : "Unknown";

                    String mediaUri = null;
                    if(fishAnglerData.has("photo")) {
                        if(fishAnglerData.getJSONObject("photo").has("mediaUri")) {
                            mediaUri = fishAnglerData.getJSONObject("photo").getString("mediaUri");
                        }
                    }

                    String[] commonNames = null;
                    if(fishAnglerData.has("commonNames")) {
                        // parse out common names from JSON array to String array
                        JSONArray commonNamesJSON = fishAnglerData.getJSONArray("commonNames");
                        commonNames = new String[commonNamesJSON.length()];
                        for (int k = 0; k < commonNamesJSON.length(); k++) {
                            commonNames[k] = commonNamesJSON.getString(k);
                        }
                    }

                    // more fish variables
                    String distribution = (fishAnglerData.has("distribution")) ? fishAnglerData.getString("distribution") : "Unknown";
                    boolean scales = fishAnglerData.getBoolean("brack");
                    boolean saltWater = fishAnglerData.getBoolean("saltwater");
                    boolean freshWater = fishAnglerData.getBoolean("fresh");
                    String coloration = (fishAnglerData.has("coloration")) ? fishAnglerData.getString("coloration") : "Unknown";
                    String feedingBehaviour = (fishAnglerData.has("feedingBehaviour")) ? fishAnglerData.getString("feedingBehaviour") : "Unknown";
                    String healthWarnings = (fishAnglerData.has("healthWarnings")) ? fishAnglerData.getString("healthWarnings") : "Unknown";
                    String foodValue = (fishAnglerData.has("foodValue")) ? fishAnglerData.getString("foodValue") : "Unknown" ;


                    // parse out names of similar species for this fish
                    String[] similarSpecies = null;
                    if(fishAnglerData.has("similarSpecies")) {
                        JSONArray similarSpeciesJSON = fishAnglerData.getJSONArray("similarSpecies");
                        similarSpecies = new String[similarSpeciesJSON.length()];
                        for (int l = 0; l < similarSpeciesJSON.length(); l++) {
                            JSONObject similarFishJSON = similarSpeciesJSON.getJSONObject(l);
                            similarSpecies[l] = similarFishJSON.getString("description");
                        }
                    }
                    // last bit of information
                    String environmentDetail = (fishAnglerData.has("environmentDetail")) ? fishAnglerData.getString("environmentDetail") : "Unknown" ;

                    // create a Fish object out of the gathered info
                    fishes.add(new Fish(
                            latinName,
                            accuracy,
                            title,
                            mediaUri,
                            commonNames,
                            distribution,
                            scales,
                            saltWater,
                            freshWater,
                            coloration,
                            feedingBehaviour,
                            healthWarnings,
                            foodValue,
                            similarSpecies,
                            environmentDetail
                    ));
                }
            }
            return fishes;
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    private JSONObject fetchAuthorizationToken() {

        try {
            JSONObject response = null;
            // specify the URL for the authorization endpoint
            URL url = new URL("https://api-users.fishial.ai/v1/auth/token");
            // establish the connection with the endpoint
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            // define the header as POST request with JSON body and allow for output
            http.setRequestMethod("POST");
            http.setRequestProperty("Content-Type", "application/json");
            http.setDoOutput(true);
            // create an JSON object and populate it with the API key and API secret key
            JSONObject fishialAPIKeys = new JSONObject();

            fishialAPIKeys.put("client_id", context.getString(R.string.FISHIAL_API_KEY));
            fishialAPIKeys.put("client_secret", context.getString(R.string.FISHIAL_API_SECRET_KEY));
            // convert the JSON objet which is written with the UTF-8 charset to an byte array
            byte[] out = fishialAPIKeys.toString().getBytes(StandardCharsets.UTF_8);
            // get an output stream of the http connection and write out the byte array
            OutputStream outStream = http.getOutputStream();
            outStream.write(out);
            outStream.close();

            // if https request was successful then read in the response into an StringBuilder object using the http input stream
            if (http.getResponseCode() == 200) {
                // create a buffered reader for reading the token data passed back from the authorization endpoint
                BufferedReader tokenDataReader = new BufferedReader(new InputStreamReader(http.getInputStream()));
                // StringBuilder to build the response
                StringBuilder httpResponse = new StringBuilder();
                // go through the token data line by line and append each line to the StringBuilder
                String line;
                while ((line = tokenDataReader.readLine()) != null) {
                    httpResponse.append(line);
                }
                tokenDataReader.close();
                // Create a response JSON object returned by this method by calling the toString() method on the created StringBuilder
                response = new JSONObject(httpResponse.toString());
            }
            // close the http connection
            http.disconnect();
            return response;
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    private JSONObject obtainDataForCloudUpload(JSONObject accessToken, FishImage fishImage) {
        try {
            JSONObject response = null;
            JSONObject mainJsonBody = new JSONObject();
            mainJsonBody.put("filename", fishImage.getImageFileName());
            mainJsonBody.put("content_type", fishImage.getImageFileMimeType());
            mainJsonBody.put("byte_size", fishImage.getImageFileSize());
            // workaround for removing "\n" from the end of the base64 checksum
            mainJsonBody.put("checksum", fishImage.getImageFileBytesArrayMD5EncodedBase64().substring(0,23) + "=");
            // wrap around the above data in to the another object called blob as required by the endpoint
            JSONObject blob = new JSONObject();
            blob.put("blob", mainJsonBody);

            // parse out the "Bearer (token)" out of the access Token JSON object
            String token = accessToken.getString("access_token");
            // specify the URL for the authorization endpoint
            URL url = new URL("https://api.fishial.ai/v1/recognition/upload");
            // establish the connection with the endpoint
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            // define the header as POST request with JSON body and allow for output
            http.setRequestMethod("POST");
            http.setRequestProperty("Accept", "application/json");
            http.setRequestProperty("Authorization", "Bearer " + token);
            http.setRequestProperty("Content-Type", "application/json");
            http.setDoOutput(true);

            // convert the JSON objet which is written with the UTF-8 charset to an byte array
            byte[] out = blob.toString().getBytes(StandardCharsets.UTF_8);
            // get an output stream of the http connection and write out the byte array
            OutputStream outStream = http.getOutputStream();
            outStream.write(out);
            outStream.close();

            if (http.getResponseCode() == 200) {
                BufferedReader responseDataReader = new BufferedReader(new InputStreamReader(http.getInputStream()));
                // StringBuilder to build the response
                StringBuilder httpResponse = new StringBuilder();
                // go through the token data line by line and append each line to the StringBuilder
                String line;
                while ((line = responseDataReader.readLine()) != null) {
                    httpResponse.append(line);
                }
                responseDataReader.close();

                response = new JSONObject(httpResponse.toString());

            }
            // close the http connection
            http.disconnect();
            return response;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void uploadAnImageToTheCloud(JSONObject dataForCloudImageUpload, FishImage fishImage) {
        try {
            // get values of the JSON object used for uploading the image to the cloud
            JSONObject directUploadHeader = dataForCloudImageUpload.getJSONObject("direct-upload");
            String urlString = directUploadHeader.getString("url");
            JSONObject imageHeaders = directUploadHeader.getJSONObject("headers");
            String contentMd5 = imageHeaders.getString("Content-MD5");
            String contentDisposition = imageHeaders.getString("Content-Disposition");

            // parse the file size in bytes to int
            int noOfBytes = Integer.parseInt(fishImage.getImageFileSize());
            // open an input stream using the content resolver and the image uri
            InputStream inputStream = fishImage.getContentResolver().openInputStream(fishImage.getFishImage());
            // byte array to hold raw image data of the same size as the image
            byte[] byteArray = new byte[noOfBytes];
            // read bytes of the image into the byte array
            inputStream.read(byteArray);
            // close the input stream
            inputStream.close();

            URL url = new URL(urlString);
            // establish the connection with the endpoint
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            // define the header as POST request with JSON body and allow for output
            http.setRequestMethod("PUT");
            http.setRequestProperty("Content-Disposition", contentDisposition);
            http.setRequestProperty("Content-MD5", contentMd5);
            http.setRequestProperty("Content-Type", "");
            http.setDoOutput(true);

            OutputStream outStream = http.getOutputStream();
            outStream.write(byteArray);
            outStream.close();

            // there shouldn't be any response in this request other than an error
            if (http.getResponseCode() != 200) {
                System.out.println("Image upload to the cloud failed");
                System.out.println(http.getResponseCode());
                System.out.println(http.getResponseMessage());
            }

            // close the http connection
            http.disconnect();

        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JSONObject fishDetection(JSONObject imageDataOnCloudJSON, JSONObject tokenJSON) {
        try {
            JSONObject response = null;
            String urlPath = imageDataOnCloudJSON.getString("signed-id");
            String token = tokenJSON.getString("access_token");

            URL url = new URL("https://api.fishial.ai/v1/recognition/image?q="+urlPath);
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            http.setRequestProperty("Authorization", "Bearer "+token);

            if (http.getResponseCode() == 200) {
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
                response = new JSONObject(httpResponse.toString());
            }
            // close the http session
            http.disconnect();
            return response;
        } catch (JSONException e) {
            throw new RuntimeException(e);}
        catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
