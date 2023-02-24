package com.example.fishcenter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;


public class MainPageActivity extends AppCompatActivity {

    private ImageButton fishRecognitionImageButtonMainPageActivity;
    private ImageButton googleMapsButtonMainPageActivity;

    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainpage);
        mAuth = FirebaseAuth.getInstance();

        fishRecognitionImageButtonMainPageActivity = findViewById(R.id.fishRecognitionImageButtonMainPageActivity);
        googleMapsButtonMainPageActivity = findViewById(R.id.googleMapsButtonMainPageActivity);

        fishRecognitionImageButtonMainPageActivity.setOnClickListener(view -> {
            Intent fishRecognitionActivity = new Intent(getApplicationContext(), FishRecognitionActivity.class);
            startActivity(fishRecognitionActivity);
        });

        googleMapsButtonMainPageActivity.setOnClickListener(view -> {
            Intent mapActivity = new Intent(getApplicationContext(), MapActivity.class);
            startActivity(mapActivity);
        });

    }





}
