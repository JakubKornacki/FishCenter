package com.example.fishcenter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;


public class MainPageActivity extends AppCompatActivity {

    private ImageButton fishRecognitionImageButton;
    private ImageButton googleMapsButton;

    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainpage);
        mAuth = FirebaseAuth.getInstance();

        fishRecognitionImageButton = findViewById(R.id.fishRecognitionImageButton);
        googleMapsButton = findViewById(R.id.googleMapsButton);

        fishRecognitionImageButton.setOnClickListener(view -> {
            Intent fishRecognitionActivity = new Intent(getApplicationContext(), FishRecognitionActivity.class);
            startActivity(fishRecognitionActivity);
        });

        googleMapsButton.setOnClickListener(view -> {
            Intent mapActivity = new Intent(getApplicationContext(), MapActivity.class);
            startActivity(mapActivity);
        });

    }

    // minimise the app when the back button is clicked on the main menu
    @Override
    public void onBackPressed() {
        this.moveTaskToBack(true);
    }





}
