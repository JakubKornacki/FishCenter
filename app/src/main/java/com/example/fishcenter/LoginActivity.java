package com.example.fishcenter;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;


public class LoginActivity extends AppCompatActivity {


    private EditText emailEditText;
    private EditText passwordEditText;
    private Button signInButton;
    private ImageButton passwordVisibleImageButton;
    private TextView userNotRegisteredText;
    private FirebaseAuth mAuth;
    private TextView forgotPasswordTextView;
    private LinearLayout mainContentLayout;

    /*
    // if the user is still authenticated then redirect him to the main page of the application
    @Override
    protected void onStart( ) {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent mainPageActivity = new Intent(getApplicationContext(), MainPageActivity.class);
            startActivity(mainPageActivity);
        }
    } */

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // get firebase auth instance
        mAuth = FirebaseAuth.getInstance();
        // change the name of this view to "Sign In" from "Fish Center", way to do it found here https://stackoverflow.com/a/29455956 changing android:label in Android Manifest did not work.
        getSupportActionBar().setTitle(R.string.signIn);
        // get references to different components visible on this activity such as editTexts and Buttons
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.retypePasswordEditText);
        signInButton = findViewById(R.id.signInButton);
        passwordVisibleImageButton = findViewById(R.id.retypePasswordVisibleImageButton);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        mainContentLayout = findViewById(R.id.mainContentLayout);

        // get a span by parsing out the HTML so that the string can be displayed as bold
        Spanned span = HtmlCompat.fromHtml(getString(R.string.userDoesNotHaveAnAccount), HtmlCompat.FROM_HTML_MODE_LEGACY);
        SpannableString spannableString = new SpannableString(span);
        //  Color in the "Register" portion of the text in the spannable string with purple color
        spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.ordinaryButtonColor)), 23,spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        // set up the text view
        userNotRegisteredText = new TextView(getApplicationContext());
        LinearLayout.LayoutParams mainContentParams = new LinearLayout.LayoutParams(mainContentLayout.getLayoutParams());
        mainContentParams.setMargins(0,40,0,0);
        userNotRegisteredText.setLayoutParams(mainContentParams);
        userNotRegisteredText.setClickable(true);
        userNotRegisteredText.setGravity(Gravity.CENTER);
        userNotRegisteredText.setMinWidth(48);
        userNotRegisteredText.setText(spannableString);
        userNotRegisteredText.setTextSize(16);
        mainContentLayout.addView(userNotRegisteredText);


        // set up the key handlers of all buttons visible on this page
        // switch from login activity to the register activity when clicked on the "Don't have an account? Register" TextView on the login activity
        userNotRegisteredText.setOnClickListener(view -> {
            Intent registerActivity = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(registerActivity);
        });

        // toggle password visibility
        passwordVisibleImageButton.setOnClickListener(view ->  {
            togglePasswordVisibilityButton(passwordVisibleImageButton, passwordEditText);
        });

        // sign in button
        signInButton.setOnClickListener(view -> {
            signInWithFirebase();
        });

        forgotPasswordTextView.setOnClickListener(view -> {
            Intent forgotPasswordActivity = new Intent(getApplicationContext(), ForgotPasswordActivity.class);
            startActivity(forgotPasswordActivity);
        });



    }


    private boolean validateEmail(String email) {
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

        // display error messages if there are any
        if(errorMessageEmail.length() != 0) {
            emailEditText.setError(errorMessageEmail.toString());
            return false;
        }
        return true;
    }

    private boolean validatePassword(String password) {
        StringBuilder errorMessagePassword = new StringBuilder();

        if(password.isEmpty()) {
            errorMessagePassword.append("* Password cannot be empty!\n");
        }

        // display error messages if there are any
        if(errorMessagePassword.length() != 0) {
            passwordEditText.setError(errorMessagePassword.toString());
            return false;
        }

        return true;
    }


    private void signInWithFirebase() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        boolean emailValidated = validateEmail(email);
        boolean passwordValidated = validatePassword(password);


        if(emailValidated && passwordValidated) {
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
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
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
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