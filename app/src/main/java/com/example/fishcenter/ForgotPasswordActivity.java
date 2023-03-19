package com.example.fishcenter;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailEditText;
    private ImageButton submitButton;
    private LinearLayout mainContentLayout;
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private LinearLayout progressSpinnerLayout;
    private LinearLayout backToLoginLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        emailEditText = findViewById(R.id.emailEditText);
        progressSpinnerLayout = findViewById(R.id.progressSpinnerLayout);

        mainContentLayout = findViewById(R.id.mainContentLayout);
        backToLoginLayout = createBackToLoginLayout(mainContentLayout);
        mainContentLayout.addView(backToLoginLayout);

        submitButton = findViewById(R.id.submitButton);
        // reset password button on click handler
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailEditText.getText().toString().trim();
                resetPasswordWithFirebase(email);
            }
        });
        // if the user click anywhere on the background linear layout which is anywhere on the screen apart from the top bar
        // the focus from the edit texts should be cleared and the input keyboard should be hidden

        LinearLayout linearLayoutBackground = findViewById(R.id.linearLayoutBackground);
        linearLayoutBackground.setOnClickListener(view -> {
            if(emailEditText.isFocused()) {
                emailEditText.clearFocus();
            }
            // //hide soft input from window to hide the keyboard
            // https://stackoverflow.com/questions/1109022/how-to-close-hide-the-android-soft-keyboard-programmatically/15587937#15587937
            // get the input keyboard and call the
            InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });

    }

    private LinearLayout createBackToLoginLayout(LinearLayout parentLayout) {
        // set layout parameter to be the parameters of the linear layout with main content
        final LinearLayout.LayoutParams parentLayoutParams = new LinearLayout.LayoutParams(parentLayout.getLayoutParams());
        // the text view to be added should be 40 pixels below the main content
        parentLayoutParams.setMargins(0,40,0,0);
        // linear layout to hold the text view which matches the layout params of main content
        final LinearLayout textLinearLayout = new LinearLayout(getApplicationContext());
        textLinearLayout.setLayoutParams(parentLayoutParams);
        textLinearLayout.setGravity(Gravity.CENTER);

        final LinearLayout.LayoutParams textLinearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        // set up the text view
        final TextView goBackToLoginText = new TextView(getApplicationContext());
        goBackToLoginText.setLayoutParams(textLinearLayoutParams);
        // define the text view appearance and position
        goBackToLoginText.setClickable(true);
        goBackToLoginText.setMinWidth(48);
        goBackToLoginText.setTextSize(16);
        goBackToLoginText.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_10_gray_opacity_30_to_transparent));
        textLinearLayout.addView(goBackToLoginText);
        // get the spannable string
        SpannableString text = createSpannableString();
        goBackToLoginText.setText(text);

        // switch from login activity to the register activity when clicked on the "Do you remember your password? Log in" TextView on the login activity
        goBackToLoginText.setOnClickListener(view -> {
            finish();
        });

        return textLinearLayout;
    }


    private SpannableString createSpannableString() {
        // get a span by parsing out the HTML so that the string can be displayed as bold
        Spanned span = HtmlCompat.fromHtml(getString(R.string.userRemembersPassword), HtmlCompat.FROM_HTML_MODE_LEGACY);
        SpannableString spannableString = new SpannableString(span);
        //  Color in the "Register" portion of the text in the spannable string with purple color
        spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.black)), 0,31, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.ordinaryButtonColor)), 31, spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }



    private boolean validateEmail(String email) {
        StringBuilder errorMessageEmail = new StringBuilder();
        // make sure email is not empty
        if(email.isEmpty()) {
            errorMessageEmail.append("E-mail cannot be empty!\n");
        }

        // email length exceeded
        if(email.length() > 320) {
            errorMessageEmail.append("E-mail length cannot exceed 320 characters!\n");
        }

        // make sure email contains '.' and '@'
        if((email.indexOf('.') == -1 ) && email.indexOf('@') == -1 ) {
            errorMessageEmail.append("E-mail needs to contain '.' and '@'!");
        }

        // display the error messages and return false, clear the error message
        if(errorMessageEmail.length() != 0) {
            emailEditText.setError(errorMessageEmail.toString());
            return false;
        }

        return true;
    }


    private void resetPasswordWithFirebase(String email) {
        boolean emailValidated = validateEmail(email);
        if(emailValidated) {
            showSpinnerAndDisableComponents(true);
            firebaseAuth.sendPasswordResetEmail(email).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Toast.makeText(ForgotPasswordActivity.this, "Your password reset e-mail is on the way!", Toast.LENGTH_LONG).show();
                    showSpinnerAndDisableComponents(false);
                }
            });
            firebaseAuth.sendPasswordResetEmail(email).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    createErrorAlertDialog("E-mail reset error:", e.getMessage());
                    showSpinnerAndDisableComponents(false);
                }
            });
        }
    }


    private void showSpinnerAndDisableComponents(boolean flag) {
        emailEditText.setFocusable(!flag);
        submitButton.setClickable(!flag);
        backToLoginLayout.getChildAt(0).setClickable(!flag);

        if(flag) {
            progressSpinnerLayout.setVisibility(View.VISIBLE);
            submitButton.setBackground(null);
            backToLoginLayout.getChildAt(0).setBackground(null);
        } else {
            progressSpinnerLayout.setVisibility(View.INVISIBLE);
            submitButton.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_30_to_transparent));
            backToLoginLayout.getChildAt(0).setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_30_to_transparent));
        }
    }

    // utility method to an error alert dialog programmatically
    private void createErrorAlertDialog(String alertTitle, String alertMessage) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(alertTitle);
        alert.setMessage(alertMessage);
        alert.setIcon(R.drawable.ic_baseline_error_outline_36_black);
        alert.show();
    }
}
