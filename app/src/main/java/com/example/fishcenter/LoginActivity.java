package com.example.fishcenter;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private EditText emailEditText;
    private EditText passwordEditText;
    private ImageButton signInButton;
    private ImageButton passwordVisibleImageButton;
    private LinearLayout progressSpinnerLayout;
    private TextView forgotPasswordTextView;
    private LinearLayout mainContentLayout;
    private LinearLayout goToRegisterLayout;


    // if the user is still authenticated then redirect him to the main page of the application
    @Override
    protected void onStart() {
        super.onStart();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // wait for 50 milliseconds before executing the transfer to the main activity
                // this gives time for the login activity to fully load on a cold start
                // before transferring to the main page and therefor avoids an ugly black screen
                // on path splash screen -> black screen -> login activity -> main page activity
                FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null) {
                    Intent mainPageActivity = new Intent(getApplicationContext(), MainPageActivity.class);
                    startActivity(mainPageActivity);
                    finish();
                }
            }
        }, 50);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // get references to different components visible on this activity such as editTexts and Buttons
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.retypePasswordEditText);
        signInButton = findViewById(R.id.signInButton);
        passwordVisibleImageButton = findViewById(R.id.retypePasswordVisibleImageButton);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        progressSpinnerLayout = findViewById(R.id.progressSpinnerLayout);

        mainContentLayout = findViewById(R.id.mainContentLayout);
        goToRegisterLayout = createBackToLoginLayout(mainContentLayout);
        mainContentLayout.addView(goToRegisterLayout);


        // toggle password visibility
        passwordVisibleImageButton.setOnClickListener(view -> togglePasswordVisibilityButton(passwordVisibleImageButton, passwordEditText));

        // sign in button
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInWithFirebase();
            }
        });

        forgotPasswordTextView.setOnClickListener(view -> {
            Intent forgotPasswordActivity = new Intent(getApplicationContext(), ForgotPasswordActivity.class);
            startActivity(forgotPasswordActivity);
        });

        // if the user click anywhere on the background linear layout which is anywhere on the screen apart from the top bar
        // the focus from the edit texts should be cleared and the input keyboard should be hidden
        LinearLayout linearLayoutBackground = findViewById(R.id.linearLayoutBackground);
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



    private LinearLayout createBackToLoginLayout(LinearLayout parentLayout) {
        // give the text view the layout parameters of the main content linear layout
        final LinearLayout.LayoutParams parentLayoutParams = new LinearLayout.LayoutParams(parentLayout.getLayoutParams());
        // the text view to be added should be 40 pixels below the main content
        parentLayoutParams.setMargins(0,40,0,0);
        // linear layout to hold the text view which matches the layout params of main content
        final LinearLayout textLinearLayout = new LinearLayout(getApplicationContext());
        textLinearLayout.setLayoutParams(parentLayoutParams);
        textLinearLayout.setGravity(Gravity.CENTER);

        final LinearLayout.LayoutParams textLinearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        // set up the text view
        final TextView userNotRegisteredText = new TextView(getApplicationContext());
        userNotRegisteredText.setLayoutParams(textLinearLayoutParams);
        // define what the text view can do, its appearance and position
        userNotRegisteredText.setClickable(true);
        userNotRegisteredText.setGravity(Gravity.CENTER);
        userNotRegisteredText.setMinWidth(48);
        // get the spannable string
        SpannableString text = createSpannableString();
        userNotRegisteredText.setText(text);
        userNotRegisteredText.setTextSize(16);
        // need a slightly higher opacity value here in comparison to other two text view in forgot password and register since the background is too bright
        userNotRegisteredText.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_10_gray_opacity_40_to_transparent));
        textLinearLayout.addView(userNotRegisteredText);

        // switch from login activity to the register activity when clicked on the "Don't have an account? Register" TextView on the login activity
        userNotRegisteredText.setOnClickListener(view -> {
            Intent registerActivity = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(registerActivity);
        });

        return textLinearLayout;
    }

    private SpannableString createSpannableString() {
        // get a span by parsing out the HTML so that the string can be displayed as bold
        Spanned span = HtmlCompat.fromHtml(getString(R.string.userDoesNotHaveAnAccount), HtmlCompat.FROM_HTML_MODE_LEGACY);
        SpannableString spannableString = new SpannableString(span);
        //  Color in the "Register" portion of the text in the spannable string with purple color
        spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.black)), 0,23, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.ordinaryButtonColor)), 23,spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    private boolean validateEmail(String email) {
        StringBuilder errorMessageEmail = new StringBuilder();

        // make sure email is not empty
        if(email.isEmpty()) {
            errorMessageEmail.append("E-mail address cannot be empty!\n");
        }

        // email length exceeded
        if(email.length() > 320) {
            errorMessageEmail.append("E-mail length cannot exceed 320 characters!\n");
        }

        // make sure email contains '.' and '@'
        if(email.indexOf('@') == -1 && email.indexOf('.') == -1) {
            errorMessageEmail.append("E-mail needs to contain '.' and '@'!");
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
            errorMessagePassword.append("Password cannot be empty!");
        }

        // display error messages if there are any
        if(errorMessagePassword.length() != 0) {
            passwordEditText.setError(errorMessagePassword.toString());
            return false;
        }

        return true;
    }


    private void signInWithFirebase() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        boolean emailValidated = validateEmail(email);
        boolean passwordValidated = validatePassword(password);

        if(emailValidated && passwordValidated) {
            showSpinnerAndDisableComponents(true);
            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        showSpinnerAndDisableComponents(false);
                        Intent mainPageActivity = new Intent(getApplicationContext(), MainPageActivity.class);
                        startActivity(mainPageActivity);
                    }
                }
            });
            firebaseAuth.signInWithEmailAndPassword(email, password).addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    showSpinnerAndDisableComponents(false);
                    AlertUtilities.createErrorAlertDialog(LoginActivity.this,"Authentication:", e.getMessage());
                }
            });
        }
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


    private void showSpinnerAndDisableComponents(boolean flag) {
        emailEditText.setEnabled(!flag);
        passwordEditText.setEnabled(!flag);
        signInButton.setClickable(!flag);
        forgotPasswordTextView.setFocusable(!flag);
        forgotPasswordTextView.setClickable(!flag);
        goToRegisterLayout.getChildAt(0).setClickable(!flag);
        if(flag) {
            progressSpinnerLayout.setVisibility(View.VISIBLE);
            signInButton.setBackground(null);
            forgotPasswordTextView.setBackground(null);
            goToRegisterLayout.getChildAt(0).setBackground(null);
        } else {
            progressSpinnerLayout.setVisibility(View.INVISIBLE);
            signInButton.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_30_to_transparent));
            forgotPasswordTextView.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_35_gray_opacity_30_to_transparent));
            goToRegisterLayout.getChildAt(0).setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_10_gray_opacity_40_to_transparent));
        }
    }

    private void removeErrorMessages() {
        emailEditText.setError(null);
        passwordEditText.setError(null);
    }


    // hide the error messages and spinners if were displayed when user decides to go back to this activity
    protected void onResume() {
        super.onResume();
        // hide the spinner only if the user is not logged in
        // otherwise the spinner should be displayed for 50 milliseconds
        if(firebaseAuth.getCurrentUser() == null) {
            showSpinnerAndDisableComponents(false);
        }
        removeErrorMessages();
        clearEditTexts();
    }

    private void clearEditTexts() {
        emailEditText.setText(null);
        passwordEditText.setText(null);
    }
}