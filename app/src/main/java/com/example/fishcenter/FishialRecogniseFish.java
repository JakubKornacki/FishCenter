package com.example.fishcenter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

public class FishialRecogniseFish extends Thread {
    private final Uri image;
    private final Context context;
    private final ContentResolver contentResolver;
    public FishialRecogniseFish(Uri image, Context context) {
        this.image = image;
        this.context = context;
        contentResolver = context.getContentResolver();
    }

    @Override
    public void run() {
        try {
            super.run();
            // get the access token to use the Fishial.AI api
            JSONObject accessToken = fetchAuthorizationToken();
            JSONObject dataForCloudImageUpload = obtainDataForCloudUpload(accessToken, image);
            uploadFishImageInCloud(dataForCloudImageUpload, image);
            JSONObject fishialImageRecognitionData = fishDetection(dataForCloudImageUpload, accessToken);
            // way of passing objects between activities using Intent with the help of serializable interface
            // https://stackoverflow.com/questions/13601883/how-to-pass-arraylist-of-objects-from-one-to-another-activity-using-intent-in-an
            // ArrayList of Fish objects is created by parsing out the JSON data
            if(fishialImageRecognitionData.getJSONArray("results").length() != 0) {
                Intent fishRecognisedIntent = new Intent(context, FishRecognisedActivity.class);
                HashSet<Fish> fishes = parseJSONToFishObjectHashSet(fishialImageRecognitionData);
                // New bundle which will be attached to the intent and passed over to the FishRecognisedActivity class
                Bundle bundle = new Bundle();
                // since Fish class is serializable we can put in the ArrayList of Fish object into the bundle and give it a key "fishes"
                bundle.putSerializable("fishes", fishes);
                // attach the Bundle object holding the ArrayList of Fish objects to the Intent
                fishRecognisedIntent.putExtra("bundle", bundle);
                // Start the activity called FishRecognisedActivity, the bundle with Fish objects should be attached and transferred over
                context.startActivity(fishRecognisedIntent);
            } else {
                throw new FishNotRecognisedException("Fish could not be recognised! Check image supplied, API credits or internet connection!");
            }
        } catch (JSONException | IOException | FishNotRecognisedException exception) {
            Intent fishNotRecognisedOrOutOfAPICredits = new Intent(context, FishNotRecognised.class);
            context.startActivity(fishNotRecognisedOrOutOfAPICredits);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    private HashSet<Fish> parseJSONToFishObjectHashSet(JSONObject fishesData) throws JSONException{
        HashSet<Fish> fishes = new HashSet<>();
        // get the species arrayList from fishesData (there is one entry for each fish identified in an image)
        for(int i = 0; i < fishesData.getJSONArray("results").length(); i++) {
            JSONObject result = fishesData.getJSONArray("results").getJSONObject(i);
            JSONArray species = result.getJSONArray("species");
            for (int j = 0; j < species.length(); j++) {
                // always first object
                JSONObject fishEntryInJSON = species.getJSONObject(0);

                // get fish description as variables
                fishEntryInJSON.getString("name");
                String latinName = fishEntryInJSON.getString("name");
                fishEntryInJSON.getString("accuracy");
                float accuracy = Float.parseFloat(fishEntryInJSON.getString("accuracy"));

                JSONObject fishData = fishEntryInJSON.getJSONObject("fishangler-data");
                String title = (fishData.has("title")) ? fishData.getString("title") : "Unknown";

                String mediaUri = null;
                if(fishData.has("photo")) {
                    if(fishData.getJSONObject("photo").has("mediaUri")) {
                        mediaUri = fishData.getJSONObject("photo").getString("mediaUri");
                    }
                }

                String[] commonNames = null;
                if(fishData.has("commonNames")) {
                    // parse out common names from JSON array to String array
                    JSONArray commonNamesJSON = fishData.getJSONArray("commonNames");
                    commonNames = new String[commonNamesJSON.length()];
                    for (int k = 0; k < commonNamesJSON.length(); k++) {
                        commonNames[k] = commonNamesJSON.getString(k);
                    }
                }

                String distribution = (fishData.has("distribution")) ? fishData.getString("distribution") : "Unknown";
                boolean scales = fishData.getBoolean("brack");
                boolean saltWater = fishData.getBoolean("saltwater");
                boolean freshWater = fishData.getBoolean("fresh");
                String coloration = (fishData.has("coloration")) ? fishData.getString("coloration") : "Unknown";
                String feedingBehaviour = (fishData.has("feedingBehaviour")) ? fishData.getString("feedingBehaviour") : "Unknown";
                String healthWarnings = (fishData.has("healthWarnings")) ? fishData.getString("healthWarnings") : "Unknown";
                String foodValue = (fishData.has("foodValue")) ? fishData.getString("foodValue") : "Unknown" ;

                // parse out names of similar species for this fish
                String[] similarSpecies = null;
                if(fishData.has("similarSpecies")) {
                    JSONArray similarSpeciesJSON = fishData.getJSONArray("similarSpecies");
                    similarSpecies = new String[similarSpeciesJSON.length()];
                    for (int l = 0; l < similarSpeciesJSON.length(); l++) {
                        JSONObject similarFishJSON = similarSpeciesJSON.getJSONObject(l);
                        similarSpecies[l] = similarFishJSON.getString("description");
                    }
                }
                // last bit of information
                String environmentDetail = (fishData.has("environmentDetail")) ? fishData.getString("environmentDetail") : "Unknown" ;

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
                        environmentDetail)
                );
            }
        }
        return fishes;
    }

    private JSONObject fetchAuthorizationToken() throws JSONException, IOException {
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
    }


    private JSONObject obtainDataForCloudUpload(JSONObject accessToken, Uri fishImage) throws JSONException, IOException, NoSuchAlgorithmException {
        JSONObject response = null;
        JSONObject mainJsonBody = new JSONObject();
        mainJsonBody.put("filename", MediaUtilities.getMediaFileName(contentResolver, fishImage));
        mainJsonBody.put("content_type", MediaUtilities.getMediaMimeType(contentResolver, fishImage));
        mainJsonBody.put("byte_size", MediaUtilities.getImageFileBytes(contentResolver, fishImage));
        // workaround for removing "\n" from the end of the base64 checksum
        byte[] imageM5EncodedBytes = MediaUtilities.getMediaMD5Checksum(contentResolver, fishImage);
        mainJsonBody.put("checksum", MediaUtilities.getBase64EncodedMedia(imageM5EncodedBytes).substring(0,23) + "=");
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
    }

    private void uploadFishImageInCloud(JSONObject pathForCloudUpload, Uri fishImage) throws JSONException, IOException {
        // get values of the JSON object used for uploading the image to the cloud
        JSONObject directUploadHeader = pathForCloudUpload.getJSONObject("direct-upload");
        String urlString = directUploadHeader.getString("url");
        JSONObject imageHeaders = directUploadHeader.getJSONObject("headers");
        String contentMd5 = imageHeaders.getString("Content-MD5");
        String contentDisposition = imageHeaders.getString("Content-Disposition");
        byte[] byteArray = MediaUtilities.getImageFileBytes(contentResolver, fishImage);
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
    }

    private JSONObject fishDetection(JSONObject imageDataOnCloudJSON, JSONObject tokenJSON) throws JSONException, IOException {
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
    }
}
