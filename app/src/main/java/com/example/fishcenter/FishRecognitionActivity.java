package com.example.fishcenter;


import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FishRecognitionActivity extends AppCompatActivity {

    private Button identifyFishButton;
    private LinearLayout fishImageLinearLayout;
    private ImageView fishImageImageView;
    private Uri originalImageUri;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private LinearLayout linearLayoutIndeterminateProgressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fish_recognition);
        identifyFishButton = findViewById(R.id.identifyFishButton);
        fishImageLinearLayout = findViewById(R.id.fishImageLinearLayout);
        fishImageImageView = findViewById(R.id.fishImageImageView);
        linearLayoutIndeterminateProgressBar = findViewById(R.id.linearLayoutIndeterminateProgressBar);

        fishImageLinearLayout.setOnClickListener(view -> {
            launchPhotoPicker();
        });

        identifyFishButton.setOnClickListener(view -> {
            // if the user has not selected an image create a toast that explains the error
            if (originalImageUri == null) {
                Toast.makeText(this, "Add an image!", Toast.LENGTH_SHORT).show();
                return;
            }
            fishImageLinearLayout.setClickable(false);
            identifyFishButton.setClickable(false);
            linearLayoutIndeterminateProgressBar.setVisibility(View.VISIBLE);
            FishImage fishImage = new FishImage(originalImageUri, getContentResolver());
            // start the new thread to fetch data about the fish
            FishialAPIFetchFishData fetchFishialRecognitionDataThread = new FishialAPIFetchFishData(fishImage, this);
            fetchFishialRecognitionDataThread.start();


            // thread that check if the Fishial requests have been completed every 200 milliseconds
            // requires a the runOnUiThread with new Runnable object that will set the identify button and image view clickable
            // it will also make the linear layout with the progress bar invisible
            // runOnUiThread is needed because only the thread that created the view hierarchy can modify the views
            // otherwise the Activity will crash and an exception will be raised
            Thread hideSpinnerAndEnableClicking = new Thread() {
                @Override
                public void run() {
                    super.run();
                    try {
                        Thread.sleep(200);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // if data has been fetched and the thread has finished
                                if(!fetchFishialRecognitionDataThread.isAlive()) {
                                    fishImageLinearLayout.setClickable(true);
                                    identifyFishButton.setClickable(true);
                                    linearLayoutIndeterminateProgressBar.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        });


        // set an event handler for ActivityResultLauncher<PickVisualMediaRequest> (Photo picker)
        // to pass in the uri of the selected image into the below lambda function
        // the image selected will then be displayed on the ui and a second reference to it will be kept for processing
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if(uri != null) {
                // Callback is invoked after the user selects a media item or closes the photo picker.
                // uri of the original image that is not transformed and scaled
                // will be used for fish recognition
                originalImageUri = uri;
                // image to be displayed on the user interface
                fishImageImageView.setImageURI(uri);
                fishImageImageView.requestLayout();
            } else {
                fishImageImageView.setImageDrawable(getDrawable(R.drawable.baseline_image_24));
                fishImageImageView.requestLayout();
            }
        });
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


}