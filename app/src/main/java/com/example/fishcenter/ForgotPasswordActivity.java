package com.example.fishcenter;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailEditText;
    private Button submitButton;
    private TextView goBackToLoginText;
    private LinearLayout mainContentLayout;
    private FirebaseAuth auth;
    private LinearLayout progressSpinnerLayout;
    private LinearLayout linearLayoutBackground;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        emailEditText = findViewById(R.id.emailEditText);
        submitButton = findViewById(R.id.submitButton);
        progressSpinnerLayout = findViewById(R.id.linearLayoutIndeterminateProgressBar);
        mainContentLayout = findViewById(R.id.mainContentLayout);
        linearLayoutBackground = findViewById(R.id.linearLayoutBackground);
        // goBackButton = findViewById(R.id.goBackButton);
        auth = FirebaseAuth.getInstance();


        // get a span by parsing out the HTML so that the string can be displayed as bold
        Spanned span = HtmlCompat.fromHtml(getString(R.string.userRemembersPassword), HtmlCompat.FROM_HTML_MODE_LEGACY);
        SpannableString spannableString = new SpannableString(span);
        //  Color in the "Register" portion of the text in the spannable string with purple color
        spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.black)), 0,31, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.ordinaryButtonColor)), 31, spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // set layout parameter to be the parameters of the linear layout with main content
        LinearLayout.LayoutParams mainContentParams = new LinearLayout.LayoutParams(mainContentLayout.getLayoutParams());
        mainContentParams.setMargins(0,40,0,0);
        // linear layout to hold the text view which matches the layout params of main content
        LinearLayout textLinearLayout = new LinearLayout(getApplicationContext());
        textLinearLayout.setLayoutParams(mainContentParams);
        textLinearLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams textLinearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        mainContentLayout.addView(textLinearLayout);
        // set up the text view
        goBackToLoginText = new TextView(getApplicationContext());
        goBackToLoginText.setLayoutParams(textLinearLayoutParams);
        // define the text view appearance and position
        goBackToLoginText.setClickable(true);
        goBackToLoginText.setGravity(Gravity.CENTER);
        goBackToLoginText.setMinWidth(48);
        goBackToLoginText.setText(spannableString);
        goBackToLoginText.setTextSize(16);
        goBackToLoginText.setBackground(getDrawable(R.drawable.layout_background_rounded_corners_toggle_10_gray_opacity_30_to_transparent));
        textLinearLayout.addView(goBackToLoginText);

        // switch from login activity to the register activity when clicked on the "Don't have an account? Register" TextView on the login activity
        goBackToLoginText.setOnClickListener(view -> {
            Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(loginActivity);
            removeErrorMessages();
        });

        // rest password button on click handler
        submitButton.setOnClickListener(view -> resetPasswordWithFirebase());
        // if the user click anywhere on the background linear layout which is anywhere on the screen apart from the top bar
        // the focus from the edit texts should be cleared and the input keyboard should be hidden
        linearLayoutBackground.setOnClickListener(view -> {
            if(emailEditText.isFocused()) {
                emailEditText.clearFocus();
            }
            // https://stackoverflow.com/questions/1109022/how-to-close-hide-the-android-soft-keyboard-programmatically/15587937#15587937
            // get the input keyboard and call the hid soft input from window to hide the keyboard
            InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });

    }

    private boolean validateEmail(String email) {
        StringBuilder errorMessageEmail = new StringBuilder();
        // make sure email is not empty
        if(email.isEmpty()) {
            errorMessageEmail.append("* E-mail cannot be empty!\n");
        }

        // email length exceeded
        if(email.length() > 320) {
            errorMessageEmail.append("* E-mail length cannot exceed 320 characters!\n");
        }

        // make sure email contains '.' and '@'
        if((email.indexOf('.') == -1 ) && email.indexOf('@') == -1 ) {
            errorMessageEmail.append("* E-mail needs to contain '.' and '@'!");
        }

        // display the error messages and return false, clear the error message
        if(errorMessageEmail.length() != 0) {
            emailEditText.setError(errorMessageEmail.toString());
            return false;
        }

        return true;
    }

    private void resetPasswordWithFirebase() {
        String email = emailEditText.getText().toString();
        boolean emailValidated = validateEmail(email);
        if(emailValidated) {
            showSpinner(true);
            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPasswordActivity.this, "Your password reset e-mail is on the way!", Toast.LENGTH_LONG).show();
                        }
                    });
            auth.sendPasswordResetEmail(email).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    createErrorAlertDialog("E-mail reset error!", e.getMessage());
                }
            });
            showSpinner(false);
        }

    }

    private void removeErrorMessages(){
        emailEditText.setError(null);
    }


    @Override
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

    private void showSpinner(boolean flag) {
        if(flag) {
            progressSpinnerLayout.setVisibility(View.VISIBLE);
            emailEditText.setClickable(false);
            submitButton.setClickable(false);
            goBackToLoginText.setClickable(false);
        } else {
            progressSpinnerLayout.setVisibility(View.INVISIBLE);
            emailEditText.setClickable(true);
            submitButton.setClickable(true);
            goBackToLoginText.setClickable(true);
        }
    }


    // utility method to an error alert dialog programmatically
    private void createErrorAlertDialog(String alertTitle, String alertMessage) {
        AlertDialog.Builder  alert = new AlertDialog.Builder(this);
        alert.setTitle(alertTitle);
        alert.setMessage(alertMessage);
        alert.setIcon(R.drawable.baseline_error_outline_36_black);
        alert.show();
    }


}
