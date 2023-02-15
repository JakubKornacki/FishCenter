package com.example.fishcenter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class MainPageActivity extends AppCompatActivity {

    private ImageButton fishRecognitionImageButtonMainPageActivity;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainpage);
        mAuth = FirebaseAuth.getInstance();

        fishRecognitionImageButtonMainPageActivity = findViewById(R.id.fishRecognitionImageButtonMainPageActivity);





        fishRecognitionImageButtonMainPageActivity.setOnClickListener(view -> {
            Intent fishRecognitionActivity = new Intent(getApplicationContext(), FishRecognitionActivity.class);
            startActivity(fishRecognitionActivity);
        });
    }





}
