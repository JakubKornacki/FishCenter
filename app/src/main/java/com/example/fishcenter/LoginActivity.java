package com.example.fishcenter;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.w3c.dom.Text;


public class LoginActivity extends AppCompatActivity {


    private EditText usernameEditTextLoginActivity;
    private EditText passwordEditTextLoginActivity;
    private Button signInButtonLoginActivity;

    private TextView registerTextViewLoginActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // get references to different components visible on this activity such as editTexts and Buttons
        getReferencesOfComponents();
        // set up the key handlers of all buttons visible on this page
        setupOnClickHandlers();

    }

    private void getReferencesOfComponents() {
        usernameEditTextLoginActivity = (EditText) findViewById(R.id.editTextUsername);
        passwordEditTextLoginActivity = (EditText) findViewById(R.id.editTextPasswordLogin);
        signInButtonLoginActivity = (Button) findViewById(R.id.buttonSignIn);
        registerTextViewLoginActivity = (TextView) findViewById(R.id.registerTextViewLogin);

    }

    private void setupOnClickHandlers() {


        // switch from login activity to the register activity when clicked on the "Don't have an account? Register" TextView on the login activity
        registerTextViewLoginActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent registerActivity = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(registerActivity);
            }
        });

    }

}