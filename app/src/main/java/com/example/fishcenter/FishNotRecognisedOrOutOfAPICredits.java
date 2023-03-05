package com.example.fishcenter;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class FishNotRecognisedOrOutOfAPICredits extends AppCompatActivity {

    private ImageButton goBackButtonToFishRecognition;
    private ImageButton goBackImageButton;
    private ImageButton logoutImageButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fish_not_recognised_or_out_of_api_credits);
        goBackButtonToFishRecognition = findViewById(R.id.goBackToFishRecognition);
        goBackImageButton = findViewById(R.id.goBackImageButton);
        logoutImageButton = findViewById(R.id.logoutImageButton);
        mAuth = FirebaseAuth.getInstance();
        // redirect the user back to the fish recognition activity
        goBackButtonToFishRecognition.setOnClickListener(view -> {
            Intent fishRecognitionIntent = new Intent(this, FishRecognitionActivity.class);
            startActivity(fishRecognitionIntent);
            finish();
        });

        logoutImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(loginActivity);
                finish();
            }
        });
    }
}
