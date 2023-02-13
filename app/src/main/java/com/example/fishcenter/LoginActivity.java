package com.example.fishcenter;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;


public class LoginActivity extends AppCompatActivity {


    private EditText editTextEmailLoginActivity;
    private EditText editTextPasswordLoginActivity;
    private Button signInButtonLoginActivity;

    private TextView registerTextViewLoginActivity;
    private ImageButton passwordVisibleImageButtonActivityLogin;
    private FirebaseAuth mAuth;

    /*
    // if the user is already logged in then load the main page activity
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Intent mainPageActivity = new Intent(getApplicationContext(), MainPageActivity.class);
            startActivity(mainPageActivity);
        }
    } */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // get references to different components visible on this activity such as editTexts and Buttons
        editTextEmailLoginActivity = findViewById(R.id.editTextEmailLoginActivity);
        editTextPasswordLoginActivity = findViewById(R.id.editTextPasswordLoginActivity);
        signInButtonLoginActivity = findViewById(R.id.buttonSignIn);
        registerTextViewLoginActivity =  findViewById(R.id.switchToLoginActivityRegisterActivity);
        passwordVisibleImageButtonActivityLogin = findViewById(R.id.passwordVisibleImageButtonActivityLogin);
        // set up the key handlers of all buttons visible on this page
        setupOnClickHandlers();
        // get firebase auth instance
        mAuth = FirebaseAuth.getInstance();

    }


    private void setupOnClickHandlers() {

        // switch from login activity to the register activity when clicked on the "Don't have an account? Register" TextView on the login activity
        registerTextViewLoginActivity.setOnClickListener(view -> {
            Intent registerActivity = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(registerActivity);
        });

        // toggle password visibility
        passwordVisibleImageButtonActivityLogin.setOnClickListener(view ->  {
            togglePasswordVisibilityButton(passwordVisibleImageButtonActivityLogin, editTextPasswordLoginActivity);
        });

        signInButtonLoginActivity.setOnClickListener(view -> {
            signInWithFirebase();
        });
    }

    private boolean verifyEmail(String email) {
        StringBuilder errorMessageEmail = new StringBuilder();

        // make sure email is not empty
        if(email.isEmpty()) {
            errorMessageEmail.append("* E-mail address cannot be empty!\n");
        }

        // email length exceeded
        if(email.length() > 320) {
            errorMessageEmail.append("* E-mail length cannot exceed 320 characters!\n");
        }

        // make sure email contains '.' and '@'
        if(email.indexOf('@') == -1 && email.indexOf('.') == -1) {
            errorMessageEmail.append("* E-mail needs to contain '.' and '@'!\n");
        }

        if(errorMessageEmail.length() != 0) {
            editTextEmailLoginActivity.setError(errorMessageEmail.toString());
            return false;
        }
        return true;
    }

    private boolean verifyPassword(String password) {
        StringBuilder errorMessagePassword = new StringBuilder();

        if(password.isEmpty()) {
            errorMessagePassword.append("* Password cannot be empty!\n");
        }

        if(errorMessagePassword.length() != 0) {
            editTextPasswordLoginActivity.setError(errorMessagePassword.toString());
            return false;
        }

        return true;
    }


    private void signInWithFirebase() {
        String email = editTextEmailLoginActivity.getText().toString();
        String password = editTextPasswordLoginActivity.getText().toString();

        boolean emailVerified = verifyEmail(email);
        boolean passwordVerified = verifyPassword(password);


        if(emailVerified && passwordVerified) {
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        // FirebaseUser user = mAuth.getCurrentUser();
                        //updateUI(user);
                        Intent mainPageActivity = new Intent(getApplicationContext(), MainPageActivity.class);
                        startActivity(mainPageActivity);
                    }
                }
            });
            mAuth.signInWithEmailAndPassword(email, password).addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    createErrorAlertDialog("Authentication error!", e.getMessage());
                }
            });
        }
    }

    private void createErrorAlertDialog(String alertTitle, String alertMessage) {
        AlertDialog.Builder  alert = new AlertDialog.Builder(this);
        alert.setTitle(alertTitle);
        alert.setMessage(alertMessage);
        alert.setIcon(R.drawable.baseline_error_outline_24);
        alert.show();
    }

    private void togglePasswordVisibilityButton(ImageButton imgBtn, TextView txtView) {
        /* switch between the active and inactive state as defined in the ic_password_visible_toggle_button.xml file
            this will switch the image of the button and will set the new transformation method of the EditText
            if null, no transformation method is specified and the password appears as plaintext on the user screen
            otherwise set a new password transformation method which makes the password appear as sequence of dots */
        if(imgBtn.isActivated()) {
            imgBtn.setActivated(false);
            txtView.setTransformationMethod(new PasswordTransformationMethod());

        } else {
            imgBtn.setActivated(true);
            txtView.setTransformationMethod(null);
        }
    }
}