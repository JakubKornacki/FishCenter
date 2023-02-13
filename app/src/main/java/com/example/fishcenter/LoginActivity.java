package com.example.fishcenter;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


public class LoginActivity extends AppCompatActivity {


    private EditText usernameEditTextLoginActivity;
    private EditText passwordEditTextLoginActivity;
    private Button signInButtonLoginActivity;

    private TextView registerTextViewLoginActivity;
    private ImageButton passwordVisibleImageButtonActivityLogin;

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
        usernameEditTextLoginActivity = findViewById(R.id.editTextUsername);
        passwordEditTextLoginActivity = findViewById(R.id.editTextPasswordLogin);
        signInButtonLoginActivity = findViewById(R.id.buttonSignIn);
        registerTextViewLoginActivity =  findViewById(R.id.switchToLoginActivityRegisterActivity);
        passwordVisibleImageButtonActivityLogin = findViewById(R.id.passwordVisibleImageButtonActivityLogin);
    }

    private void setupOnClickHandlers() {

        // switch from login activity to the register activity when clicked on the "Don't have an account? Register" TextView on the login activity
        registerTextViewLoginActivity.setOnClickListener(view -> {
            Intent registerActivity = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(registerActivity);
        });

        // toggle password visibility
        passwordVisibleImageButtonActivityLogin.setOnClickListener(view ->  {
            togglePasswordVisibilityButton();
        });

    }

    private void togglePasswordVisibilityButton() {
        /* switch between the active and inactive state as defined in the ic_password_visible_toggle_button.xml file
            this will switch the image of the button and will set the new transformation method of the EditText
            if null, no transformation method is specified and the password appears as plaintext on the user screen
            otherwise set a new password transformation method which makes the password appear as sequence of dots */
        if(passwordVisibleImageButtonActivityLogin.isActivated()) {
            passwordVisibleImageButtonActivityLogin.setActivated(false);
            passwordEditTextLoginActivity.setTransformationMethod(null);
            passwordEditTextLoginActivity.setTransformationMethod(new PasswordTransformationMethod());

        } else {
            passwordVisibleImageButtonActivityLogin.setActivated(true);
            passwordEditTextLoginActivity.setTransformationMethod(null);
        }
    }



}