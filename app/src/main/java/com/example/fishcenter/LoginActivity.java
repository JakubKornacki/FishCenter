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
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import com.google.firebase.auth.FirebaseUser;


public class LoginActivity extends AppCompatActivity {


    private EditText emailEditText;
    private EditText passwordEditText;
    private Button signInButton;
    private ImageButton passwordVisibleImageButton;
    private TextView userNotRegisteredText;
    private FirebaseAuth mAuth;
    private TextView forgotPasswordTextView;
    private LinearLayout mainContentLayout;
    private LinearLayout linearLayoutBackground;
    private LinearLayout progressSpinnerLayout;



    // if the user is still authenticated then redirect him to the main page of the application
    @Override
    protected void onStart( ) {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent mainPageActivity = new Intent(getApplicationContext(), MainPageActivity.class);
            startActivity(mainPageActivity);
        }
    }

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
        progressSpinnerLayout = findViewById(R.id.linearLayoutIndeterminateProgressBar);
        linearLayoutBackground = findViewById(R.id.linearLayoutBackground);
        // get a span by parsing out the HTML so that the string can be displayed as bold
        Spanned span = HtmlCompat.fromHtml(getString(R.string.userDoesNotHaveAnAccount), HtmlCompat.FROM_HTML_MODE_LEGACY);
        SpannableString spannableString = new SpannableString(span);
        //  Color in the "Register" portion of the text in the spannable string with purple color
        spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.black)), 0,23, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.ordinaryButtonColor)), 23,spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // give the text view the layout parameters of the main content linear layout
        LinearLayout.LayoutParams mainContentParams = new LinearLayout.LayoutParams(mainContentLayout.getLayoutParams());
        mainContentParams.setMargins(0,40,0,0);
        // linear layout to hold the text view which matches the layout params of main content
        LinearLayout textLinearLayout = new LinearLayout(getApplicationContext());
        textLinearLayout.setLayoutParams(mainContentParams);
        textLinearLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams textLinearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        mainContentLayout.addView(textLinearLayout);
        // set up the text view
        userNotRegisteredText = new TextView(getApplicationContext());
        userNotRegisteredText.setLayoutParams(textLinearLayoutParams);
        // define what the text view can do, its appearance and position
        userNotRegisteredText.setClickable(true);
        userNotRegisteredText.setGravity(Gravity.CENTER);
        userNotRegisteredText.setMinWidth(48);
        userNotRegisteredText.setText(spannableString);
        userNotRegisteredText.setTextSize(16);
        // need a slightly higher opacity value here in comparison to other two text view in forgot password and register since the background is too bright
        userNotRegisteredText.setBackground(getDrawable(R.drawable.layout_background_rounded_corners_toggle_10_gray_opacity_40_to_transparent));
        textLinearLayout.addView(userNotRegisteredText);


        // switch from login activity to the register activity when clicked on the "Don't have an account? Register" TextView on the login activity
        userNotRegisteredText.setOnClickListener(view -> {
            Intent registerActivity = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(registerActivity);
            removeErrorMessages();
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
            removeErrorMessages();
        });

        // if the user click anywhere on the background linear layout which is anywhere on the screen apart from the top bar
        // the focus from the edit texts should be cleared and the input keyboard should be hidden
        linearLayoutBackground.setOnClickListener(view -> {
            if(emailEditText.isFocused()) {
                emailEditText.clearFocus();
            } else if(passwordEditText.isFocused()) {
                passwordEditText.clearFocus();
            }
            // https://stackoverflow.com/questions/1109022/how-to-close-hide-the-android-soft-keyboard-programmatically/15587937#15587937
            // get the soft input keyboard from the context via the input_method_service service and hide it
            InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });
    }

    private void removeErrorMessages() {
        emailEditText.setError(null);
        passwordEditText.setError(null);
    }

    protected void onStop() {
        super.onStop();
        showSpinner(true);
        removeErrorMessages();
    }

    protected void onResume() {
        super.onResume();
        showSpinner(false);
        removeErrorMessages();
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
            showSpinner(true);
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
                    createErrorAlertDialog("Authentication:", e.getMessage());
                }
            });
            showSpinner(false);
        }
    }

    private void showSpinner(boolean flag) {
        if(flag) {
            progressSpinnerLayout.setVisibility(View.VISIBLE);
            emailEditText.setClickable(false);
            passwordEditText.setClickable(false);
            signInButton.setClickable(false);
            userNotRegisteredText.setClickable(false);
        } else {
            progressSpinnerLayout.setVisibility(View.INVISIBLE);
            emailEditText.setClickable(true);
            passwordEditText.setClickable(true);
            signInButton.setClickable(true);
            userNotRegisteredText.setClickable(true);
        }
    }

    private void createErrorAlertDialog(String alertTitle, String alertMessage) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(alertTitle);
        alert.setMessage(alertMessage);
        alert.setIcon(R.drawable.baseline_error_outline_36_black);
        alert.show();
    }

    private void togglePasswordVisibilityButton(ImageButton imgBtn, EditText editTextPass) {
        // switch between the active and inactive state as defined in the ic_password_visible_toggle_button.xml file
        //  this will switch the image of the button and will set the new transformation method of the EditText
        // if null, no transformation method is specified and the password appears as plaintext on the user screen
        // otherwise set a new password transformation method which makes the password appear as sequence of dots
        if(imgBtn.isActivated()) {
            imgBtn.setActivated(false);
            editTextPass.setTransformationMethod(new PasswordTransformationMethod());

        } else {
            imgBtn.setActivated(true);
            editTextPass.setTransformationMethod(null);
        }
        // set text pointer to the position at the end of text changing the transformation method sets it back to zero
        int textLen = editTextPass.getText().length();
        editTextPass.setSelection(textLen);
    }
}