package com.example.fishcenter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class FishNotRecognisedOrOutOfAPICredits extends AppCompatActivity {

    private ImageButton goBackButtonFishNotRecognisedActivity;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fish_not_recognised_or_out_of_api_credits);
        goBackButtonFishNotRecognisedActivity = findViewById(R.id.goBackButtonFishNotRecognisedActivity);
        // redirect the user back to the fish recognition activity
        goBackButtonFishNotRecognisedActivity.setOnClickListener(view -> {
            Intent fishRecognitionIntent = new Intent(this, FishRecognitionActivity.class);
            startActivity(fishRecognitionIntent);
        });
    }
}
