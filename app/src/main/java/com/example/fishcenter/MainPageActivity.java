package com.example.fishcenter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;


public class MainPageActivity extends AppCompatActivity {

    private ImageButton fishRecognitionImageButton;
    private ImageButton googleMapsButton;
    private ImageButton logoutImageButton;
    private Toolbar toolbar;
    private EditText postEditTextDummy;
    private EditText postEditTextActual;
    private ImageButton postSendImageButton;
    private LinearLayout postParentLinearLayout;

    private FirebaseAuth mAuth;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    private ImageView userImageView;
    private InputMethodManager keyboard;
    private LinearLayout postLinearLayoutActual;
    private boolean imageSelected;
    private Uri originalImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainpage);
        mAuth = FirebaseAuth.getInstance();
        fishRecognitionImageButton = findViewById(R.id.fishRecognitionImageButton);
        googleMapsButton = findViewById(R.id.googleMapsButton);
        logoutImageButton = findViewById(R.id.logoutImageButton);
        postEditTextDummy = findViewById(R.id.postEditTextDummy);
        postParentLinearLayout = findViewById(R.id.postParentLinearLayout);
        postEditTextActual = findViewById(R.id.postEditTextActual);
        postSendImageButton = findViewById(R.id.postSendImageButton);
        postLinearLayoutActual = findViewById(R.id.postLinearLayoutActual);
        userImageView = findViewById(R.id.userImage);
        keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        // https://developer.android.com/develop/ui/views/components/appbar/setting-up
        // need to find custom defined toolbar in the xml and replace the vanilla toolbar with it
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        userImageView.setOnClickListener(view -> launchPhotoPicker());



        postSendImageButton.setOnClickListener(view -> createPost());

        // hide the dummy post edit text, bring up the actual with the send button, force the keyboard to show up and bring focus on the actual edit text
        postEditTextDummy.setOnTouchListener((view, motionEvent) -> {
            postEditTextDummy.clearFocus();
            showActualPostTextArea(true);
            postEditTextActual.requestFocus();
            keyboard.showSoftInput(postEditTextActual, InputMethodManager.SHOW_IMPLICIT);
            return true;
        });

        postParentLinearLayout.setOnClickListener(view -> {
            postEditTextActual.clearFocus();
            showActualPostTextArea(false);
            keyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });

        logoutImageButton.setOnClickListener(view -> {
            mAuth.signOut();
            Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(loginActivity);
        });

        fishRecognitionImageButton.setOnClickListener(view -> {
            Intent fishRecognitionActivity = new Intent(getApplicationContext(), FishRecognitionActivity.class);
            startActivity(fishRecognitionActivity);
        });

        googleMapsButton.setOnClickListener(view -> {
            Intent mapActivity = new Intent(getApplicationContext(), MapActivity.class);
            startActivity(mapActivity);
        });



        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if(uri != null) {
                imageSelected = true;
                originalImageUri = uri;
                // use glide to load the selected image into the userImage, round its corners with radius 5 pixes and resize to 36x36
                Glide.with(getApplicationContext()).load(uri).transform(new RoundedCorners(5)).apply(new RequestOptions().override(36,36)).into(userImageView);
            } else {
                imageSelected = false;
                Glide.with(getApplicationContext()).load(getDrawable(R.drawable.baseline_image_search_36_black)).into(userImageView);
            }
        });

    }

    private void createPost() {
        String post = postEditTextActual.getText().toString();
        boolean postValidated = validatePost(post);
        // clear the post
        postEditTextActual.setText(null);
        postEditTextActual.setError(null);
        showActualPostTextArea(false);
        Glide.with(getApplicationContext()).load(getDrawable(R.drawable.baseline_image_search_36_black)).into(userImageView);
        originalImageUri = null;
    }

    // toggle between showing the actual post edit text and the dummy one
    private void showActualPostTextArea(boolean flag) {
        if(flag) {
            postLinearLayoutActual.setVisibility(View.VISIBLE);
            postEditTextDummy.setVisibility(View.INVISIBLE);
        } else  {
            postLinearLayoutActual.setVisibility(View.GONE);
            postEditTextDummy.setVisibility(View.VISIBLE);
        }
    }


    // minimise the app when the back button is clicked on the main menu
    @Override
    public void onBackPressed() {
        this.moveTaskToBack(true);
    }

    // hide the error messages if were displayed when user decides to go back to this activity
    protected void onResume() {
        super.onResume();
        removeErrorMessages();
        clearEditTexts();
    }

    private boolean validatePost(String post) {
        if(post.length() == 0) {
            postEditTextActual.setError("Post cannot be empty!");
            return false;
        }

        return true;
    }

    private void clearEditTexts() {
        postEditTextActual.setText(null);
    }

    private void removeErrorMessages() {
        postEditTextActual.setError(null);
    }

    private void launchPhotoPicker() {
        pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
    }



}
