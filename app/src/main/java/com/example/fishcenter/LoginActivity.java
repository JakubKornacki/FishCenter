package com.example.fishcenter;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class LoginActivity extends AppCompatActivity {


    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // get references to different components visible on this activity such as editTexts and Buttons
        getReferencesOfComponents();
        // set up the key handlers of all buttons visible on this page
        setupOnClickHandlers();

    }

    protected void getReferencesOfComponents() {
        usernameEditText = (EditText) findViewById(R.id.editTextUsername);
        passwordEditText = (EditText) findViewById(R.id.editTextPassword);
        signInButton = (Button) findViewById(R.id.buttonSignIn);
    }

    protected void setupOnClickHandlers() {


        // handle usernameEditText onclick action
        usernameEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        // handle passwordEditText onclick action
        passwordEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });


        // handle userNameEditText onclick action
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });




    }

}