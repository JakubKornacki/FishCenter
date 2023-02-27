package com.example.fishcenter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class FishNotRecognisedOrOutOfAPICredits extends AppCompatActivity {

    private ImageButton goBackButtonToFishRecognition;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fish_not_recognised_or_out_of_api_credits);
        goBackButtonToFishRecognition = findViewById(R.id.goBackToFishRecognition);
        // redirect the user back to the fish recognition activity
        goBackButtonToFishRecognition.setOnClickListener(view -> {
            Intent fishRecognitionIntent = new Intent(this, FishRecognitionActivity.class);
            startActivity(fishRecognitionIntent);
        });
    }
}
