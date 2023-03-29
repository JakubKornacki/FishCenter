package com.example.fishcenter;



import android.annotation.SuppressLint;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;

public class FishRecognitionActivity extends AppCompatActivity {

    private Button identifyFishButton;
    private LinearLayout fishImageLinearLayout;
    private ImageView fishImageImageView;
    private Uri userFishImage;
    private LinearLayout progressSpinnerLayout;
    private boolean imageSelected;
    private ImageButton goBackImageButton;
    private ImageButton logoutImageButton;
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fish_recognition);
        identifyFishButton = findViewById(R.id.identifyFishButton);
        fishImageLinearLayout = findViewById(R.id.fishImageLinearLayout);
        fishImageImageView = findViewById(R.id.fishImageImageView);
        progressSpinnerLayout = findViewById(R.id.progressSpinnerLayout);
        goBackImageButton = findViewById(R.id.goBackImageButton);
        logoutImageButton = findViewById(R.id.logoutImageButton);
        // allow for the fish image to be clipped
        fishImageImageView.setClipToOutline(true);

        goBackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        logoutImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertUtilities.createLogoutDialog(FishRecognitionActivity.this, firebaseAuth);
            }
        });

        identifyFishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if the user has not selected an image create a toast that explains the error
                if (imageSelected) {
                    FishImage fishImage = new FishImage(userFishImage, getContentResolver());
                    if(MediaUtilities.supportedImageMimeTypes.contains(fishImage.getImageFileMimeType())) {
                        showSpinnerAndDisableComponents(true);
                        // start the new thread to fetch data about the fish
                        FishialRecogniseFish fetchFishialRecognitionDataThread = new FishialRecogniseFish(fishImage, FishRecognitionActivity.this);
                        fetchFishialRecognitionDataThread.start();
                    } else {
                        Toast.makeText(getBaseContext(), "Unsupported file format!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getBaseContext(), "Load in your image!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // set an event handler for ActivityResultLauncher<PickVisualMediaRequest> (Photo picker)
        // to pass in the uri of the selected image into the below lambda function
        // the image selected will then be displayed on the ui and a second reference to it will be kept for processing
        // this is launched is invoked after the user selects a media item or closes the photo picker.
        ActivityResultLauncher<PickVisualMediaRequest> pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if(uri != null) {
                imageSelected = true;
                // uri of the original image that is not transformed and scaled and will be used for fish recognition
                userFishImage = uri;
                // use Glide to load the image into the fish image view with its uri and set the image rounded corners to have radius of 100 pixels
                Glide.with(getApplicationContext()).load(uri).override(Target.SIZE_ORIGINAL,Target.SIZE_ORIGINAL).into(fishImageImageView);
            } else {
                imageSelected = false;
                Glide.with(getApplicationContext()).load(getDrawable(R.drawable.ic_baseline_empty_image_360_gray)).into(fishImageImageView);
            }
        });

        // if the linear layout area where the fish image will be located is clicked then launch the photo picker
        fishImageLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPhotoPicker(pickMedia);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        // disable the spinner if the user decides to go back to this activity
        showSpinnerAndDisableComponents(false);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void showSpinnerAndDisableComponents(boolean flag) {
        identifyFishButton.setClickable(!flag);
        goBackImageButton.setClickable(!flag);
        fishImageLinearLayout.setEnabled(!flag);
        if(flag) {
            progressSpinnerLayout.setVisibility(View.VISIBLE);
            identifyFishButton.setBackground(null);
            goBackImageButton.setBackground(null);
        } else {
            progressSpinnerLayout.setVisibility(View.INVISIBLE);
            identifyFishButton.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_30_to_transparent));
            goBackImageButton.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_25_to_transparent));
        }
    }

    // uses Android Photo Picker to get images from user's photo gallery which is a safe way of only loading
    // in the pictures that the user has selected. Run the Photo Picker in a mode which allows to select only one
    // image from the gallery and the image needs to be an image
    // https://developer.android.com/training/data-storage/shared/photopicker
    private void launchPhotoPicker(ActivityResultLauncher<PickVisualMediaRequest> pickMedia) {
        // Launch the photo picker and allow the user to choose only images.
        // although the compiler complains about passing an invalid type to the setMediaType method the Photo Picker works fine and the application runs without crashing
        pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
    }
}