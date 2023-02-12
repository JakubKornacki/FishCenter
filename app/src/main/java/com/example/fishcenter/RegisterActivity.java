package com.example.fishcenter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class RegisterActivity extends AppCompatActivity {

    private TextView loginTextViewRegisterActivity;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        getReferencesOfComponents();
        setupOnClickHandlers();
    }
    private void setupOnClickHandlers() {


        // switch from login activity to the register activity when clicked on the "Don't have an account? Register" TextView on the login activity
        loginTextViewRegisterActivity.setOnClickListener(view -> {
            Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(loginActivity);
        });



    }


    private void getReferencesOfComponents() {
        loginTextViewRegisterActivity = findViewById(R.id.registerTextViewLogin);
    }

}
