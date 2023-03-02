package com.example.fishcenter;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

public class CreatePost extends AppCompatActivity {

   private ImageButton postSendImageButton;
   private FloatingActionButton selectImage;
   private ImageView userImageView;
   private boolean imageSelected;

   private EditText postEditText;
   private LinearLayout linearLayoutBackground;
   private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
   private InputMethodManager keyboard;
   private ImageButton goBackImageButton;
   private ImageButton logoutImageButton;
   private FirebaseAuth mAuth;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);
        linearLayoutBackground = findViewById(R.id.linearLayoutBackground);
        postSendImageButton = findViewById(R.id.postSendImageButton);
        selectImage = findViewById(R.id.selectImage);
        userImageView = findViewById(R.id.userImageView);
        postEditText = findViewById(R.id.postEditText);
        goBackImageButton = findViewById(R.id.goBackImageButton);
        logoutImageButton = findViewById(R.id.logoutImageButton);
        mAuth = FirebaseAuth.getInstance();
        keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);


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


        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPhotoPicker();
            }
        });

        postSendImageButton.setOnClickListener(view -> {
            boolean postValidated = postValidated();

            if (postValidated) {
                createPost();
            }

        });

        linearLayoutBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(postEditText.isFocused()) {
                    postEditText.clearFocus();
                    keyboard.hideSoftInputFromWindow(view.getWindowToken(),0);
                }
            }
        });


        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if(uri != null) {
                Glide.with(getApplicationContext()).load(uri).transform(new RoundedCorners(10)).override(360,360).into(userImageView);
                userImageView.setVisibility(View.VISIBLE);
                imageSelected = true;
            } else {
                userImageView.setVisibility(View.GONE);
                imageSelected = false;
            }
        });

    }


    private boolean postValidated() {
        return false;
    }

    private void createPost() {


    }

    private void launchPhotoPicker () {
        // Launch the photo picker and allow the user to choose only images.
        // although the compiler complains about passing an invalid type to the setMediaType method the Photo Picker works fine and the application runs without crashing
        pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
    }



}
