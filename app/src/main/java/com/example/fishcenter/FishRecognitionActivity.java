package com.example.fishcenter;



import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;
import java.util.HashSet;

public class FishRecognitionActivity extends AppCompatActivity {

    private Button identifyFishButton;
    private LinearLayout fishImageLinearLayout;
    private ImageView fishImageImageView;
    private Uri originalImageUri;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private LinearLayout linearLayoutIndeterminateProgressBar;
    private boolean imageSelected;
    private ImageButton goBackImageButton;
    private ImageButton logoutImageButton;
    private FirebaseAuth mAuth;
    private HashSet<String> imageMimeTypes = new HashSet<>(Arrays.asList("image/jpg","image/jpeg","image/png","image/bmp", "image/avif"));


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fish_recognition);
        identifyFishButton = findViewById(R.id.identifyFishButton);
        fishImageLinearLayout = findViewById(R.id.fishImageLinearLayout);
        fishImageImageView = findViewById(R.id.fishImageImageView);
        linearLayoutIndeterminateProgressBar = findViewById(R.id.linearLayoutIndeterminateProgressBar);
        goBackImageButton = findViewById(R.id.goBackImageButton);
        logoutImageButton = findViewById(R.id.logoutImageButton);
        mAuth = FirebaseAuth.getInstance();
        // allow for the fish image to be clipped
        fishImageImageView.setClipToOutline(true);

        goBackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mainActivity = new Intent(getApplicationContext(), MainPageActivity.class);
                startActivity(mainActivity);
            }
        });

        logoutImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(loginActivity);
            }
        });

        // if the linear layout area where the fish image will be located is clicked then launch the photo picker
        fishImageLinearLayout.setOnClickListener(view -> {
            launchPhotoPicker();
        });

        identifyFishButton.setOnClickListener(view -> {
            // if the user has not selected an image create a toast that explains the error
            if (imageSelected) {
                showSpinner(true);
                FishImage fishImage = new FishImage(originalImageUri, getContentResolver());
                if(imageMimeTypes.contains(fishImage.getImageFileMimeType())) {
                    // start the new thread to fetch data about the fish
                    FishialAPIFetchFishData fetchFishialRecognitionDataThread = new FishialAPIFetchFishData(fishImage, this);
                    fetchFishialRecognitionDataThread.start();
                } else {
                    Toast.makeText(getBaseContext(), "Unsupported file format!", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(getBaseContext(), "Cannot post an empty image!", Toast.LENGTH_SHORT).show();
            }

            });


        // set an event handler for ActivityResultLauncher<PickVisualMediaRequest> (Photo picker)
        // to pass in the uri of the selected image into the below lambda function
        // the image selected will then be displayed on the ui and a second reference to it will be kept for processing
        // this is launched is invoked after the user selects a media item or closes the photo picker.
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if(uri != null) {
                imageSelected = true;
                // uri of the original image that is not transformed and scaled and will be used for fish recognition
                originalImageUri = uri;
                // use Glide to load the image into the fish image view with its uri and set the image rounded corners to have radius of 100 pixels
                Glide.with(getApplicationContext()).load(uri).override(Target.SIZE_ORIGINAL,Target.SIZE_ORIGINAL).into(fishImageImageView);
            } else {
                imageSelected = false;
                Glide.with(getApplicationContext()).load(getDrawable(R.drawable.baseline_empty_image_360_gray)).into(fishImageImageView);
            }
        });
    }


    // thread that check if the Fishial requests have been completed every 200 milliseconds
    // requires a the runOnUiThread with new Runnable object that will set the identify button and image view clickable
    // it will also make the linear layout with the progress bar invisible
    // runOnUiThread is needed because only the thread that created the view hierarchy can modify the views
    // otherwise the Activity will crash and an exception will be raised
    private void showSpinner(boolean flag) {
        if(flag) {
            fishImageLinearLayout.setClickable(false);
            identifyFishButton.setClickable(false);
            linearLayoutIndeterminateProgressBar.setVisibility(View.VISIBLE);
        } else {
            fishImageLinearLayout.setClickable(true);
            identifyFishButton.setClickable(true);
            linearLayoutIndeterminateProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    // hide the spinner if the user decides to go back to this activity from the results page
    protected void onResume() {
        super.onResume();
        showSpinner(false);
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