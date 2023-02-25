package com.example.fishcenter;


import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import android.provider.OpenableColumns;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FishRecognitionActivity extends AppCompatActivity {

    private Button recognizeFishButtonFishRecognitionActivity;
    private Button addImageButtonFishRecognitionActivity;
    private ImageView imageViewFishRecognitionActivity;
    private Uri originalImageUri;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private JSONObject fishialImageRecognitionData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fish_recognition);
        recognizeFishButtonFishRecognitionActivity = findViewById(R.id.recognizeFishButtonFishRecognitionActivity);
        imageViewFishRecognitionActivity = findViewById(R.id.imageViewFishRecognitionActivity);
        addImageButtonFishRecognitionActivity = findViewById(R.id.addImageButtonFishRecognitionActivity);

        addImageButtonFishRecognitionActivity.setOnClickListener(view -> {
            launchPhotoPicker();
        });

        recognizeFishButtonFishRecognitionActivity.setOnClickListener(view -> {
            // if the user has not selected an image create a toast that explains the error
            if (originalImageUri == null) {
                Toast.makeText(this, "Add an image!", Toast.LENGTH_SHORT).show();
                return;
            }
            Thread recognizeFishThread = new Thread() {
                @Override
                public void run() {
                    // get the access token to use the Fishial.AI api
                    JSONObject accessToken = fetchAuthorizationToken();
                    // get image mime type
                    String imageMimeType = getImageMimeType(originalImageUri);
                    // get image name with .mimetype
                    String imageName = getImageName(originalImageUri);
                    // get image size in bytes
                    String imageSize = getImageSize(originalImageUri);
                    // get md5 checksum of the image in bytes
                    byte[] imageMD5BytesChecksum = getImageMD5ByteChecksum(imageSize);
                    // encode the md5 byte checksum in Base64 with the default Base64 setting
                    String base64EncodedMD5Checksum = Base64.encodeToString(imageMD5BytesChecksum, Base64.DEFAULT);
                    // obtain data which will be used for uploading the image onto the cloud
                    JSONObject dataForCloudImageUpload = obtainDataForCloudUpload(accessToken, imageName, imageMimeType, imageSize, base64EncodedMD5Checksum);
                    uploadAnImageToTheCloud(dataForCloudImageUpload, imageSize);
                    fishialImageRecognitionData = fishDetection(dataForCloudImageUpload, accessToken);
                    System.out.println(fishialImageRecognitionData);
                }
            };
            recognizeFishThread.start();
        });

        // set an event handler for ActivityResultLauncher<PickVisualMediaRequest> (Photo picker)
        // to pass in the uri of the selected image into the below lambda function
        // the image selected will then be displayed on the ui and a second reference to it will be kept for processing
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            // Callback is invoked after the user selects a media item or closes the photo picker.
            // uri of the original image that is not transformed and scaled
            // will be used for fish recognition
            originalImageUri = uri;
            // image to be displayed on the user interface
            imageViewFishRecognitionActivity.setImageURI(uri);
            imageViewFishRecognitionActivity.setScaleType(ImageView.ScaleType.FIT_XY);
        });

    }

    private JSONObject fetchAuthorizationToken() {
        JSONObject response = null;
        try {
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
            fishialAPIKeys.put("client_id", getString(R.string.FISHIAL_API_KEY));
            fishialAPIKeys.put("client_secret", getString(R.string.FISHIAL_API_SECRET_KEY));
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

        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }

        return response;
    }


    // uses Android Photo Picker to get images from user's photo gallery which is a safe way of only loading
    // in the pictures that the user has selected. Run the Photo Picker in a mode which allows to select only one
    // image from the gallery and the image needs to be an image
    // https://developer.android.com/training/data-storage/shared/photopicker
    private void launchPhotoPicker() {
        // Launch the photo picker and allow the user to choose only images.
        // although the compiler complains about passing an invalid type to the setMediaType method the Photo Picker works fine and the application runs without crashing
        pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
    }


    // use the ContentResolver and the image Uri obtained earlier to extract the mime type of the image
    //https://developer.android.com/training/secure-file-sharing/retrieve-info
    private String getImageMimeType(Uri imageUri) {
        ContentResolver contentResolver = this.getContentResolver();
        String imageMimeType = contentResolver.getType(imageUri);
        return imageMimeType;
    }


    private String getImageName(Uri originalImageUri) {
        // create a cursor object to point at the image data that is returned by the content resolver query which is supplied with the image uri
        Cursor cursor = getContentResolver().query(originalImageUri, null, null, null, null);
        // move the first element initially it points to 0 which points to no element now the cursor is able to fetch the data wanted when queried with the column index
        cursor.moveToFirst();
        // get the index of the column which contains the display name of the file
        int fileNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        // get the element at the column with index fileNameIndex
        String fileName = cursor.getString(fileNameIndex);
        return fileName;
    }

    private String getImageSize(Uri originalImageUri) {
        // create a cursor object to point at the image data that is returned by the content resolver query which is supplied with the image uri
        Cursor cursor = getContentResolver().query(originalImageUri, null, null, null, null);
        // move the first element initially it points to 0 which points to no element now the cursor is able to fetch the data wanted when queried with the column index
        cursor.moveToFirst();
        // get the index of the column which contains the size of the file
        int imageSizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
        // get the element at the column with index imageSizeIndex
        String imageSize = cursor.getString(imageSizeIndex);
        return imageSize;
    }


    // below implementation is based on the information found on GeeksForGeeks https://www.geeksforgeeks.org/md5-hash-in-java/?ref=rp
    private byte[] getImageMD5ByteChecksum(String fileSizeInBytes) {
        byte[] md5DigestedBytes;
        try {
            // parse the file size in bytes to int
            int noOfBytes = Integer.parseInt(fileSizeInBytes);
            // open an input stream using the content resolver and the image uri
            InputStream inputStream = getContentResolver().openInputStream(originalImageUri);
            // byte array to hold raw image data of the same size as the image
            byte[] byteArray = new byte[noOfBytes];
            // read bytes of the image into the byte array
            inputStream.read(byteArray);
            // close the input stream
            inputStream.close();
            // get an instance of the MD5 hash alg.
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            // get bytes digested by MD5
            md5DigestedBytes = md5.digest(byteArray);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return md5DigestedBytes;
    }

    private JSONObject obtainDataForCloudUpload(JSONObject accessToken, String imageName, String imageMimeType, String imageSize, String base64EncodedMD5Checksum) {
        JSONObject response = null;
        try {
            JSONObject mainJsonBody = new JSONObject();
            mainJsonBody.put("filename", imageName);
            mainJsonBody.put("content_type", imageMimeType);
            mainJsonBody.put("byte_size", imageSize);
            // workaround for removing "\n" from the end of the base64 checksum
            mainJsonBody.put("checksum", base64EncodedMD5Checksum.substring(0,23) + "=");
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

        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    private void uploadAnImageToTheCloud(JSONObject dataForCloudImageUpload, String fileSizeInBytes) {
        try {
            // get values of the JSON object used for uploading the image to the cloud
            JSONObject directUploadHeader = dataForCloudImageUpload.getJSONObject("direct-upload");
            String urlString = directUploadHeader.getString("url");
            JSONObject imageHeaders = directUploadHeader.getJSONObject("headers");
            String contentMd5 = imageHeaders.getString("Content-MD5");
            String contentDisposition = imageHeaders.getString("Content-Disposition");


            // parse the file size in bytes to int
            int noOfBytes = Integer.parseInt(fileSizeInBytes);
            // open an input stream using the content resolver and the image uri
            InputStream inputStream = getContentResolver().openInputStream(originalImageUri);
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
                System.out.println(http.getResponseCode());
                System.out.println(http.getResponseMessage());
            } else {
                System.out.println("Image uploaded");
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
        JSONObject response = null;
        try {
            System.out.println(imageDataOnCloudJSON);
            System.out.println(tokenJSON);
            String urlPath = imageDataOnCloudJSON.getString("signed-id");
            String token = tokenJSON.getString("access_token");
            URL url = new URL("https://api.fishial.ai/v1/recognition/image?q=" + urlPath);
            // establish the connection with the endpoint
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("GET");
            http.setRequestProperty("Authorization", "Bearer " + token);
            http.setRequestProperty("Accept", "application/json");
            http.setDoOutput(true);

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
            } else {
                System.out.println(http.getResponseCode());
                System.out.println(http.getResponseMessage());
            }

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
        return response;
    }

}