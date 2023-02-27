package com.example.fishcenter;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import com.google.android.gms.tasks.*;
import com.google.firebase.auth.*;


public class RegisterActivity extends AppCompatActivity {

    // firebase authentication object instance
    private FirebaseAuth mAuth;
    private LinearLayout mainContentLayout;
    private TextView userAlreadyRegistered;
    private Button signUpButton;
    private EditText emailEditText;
    private EditText nicknameEditText;
    private EditText passwordEditText;
    private EditText retypePasswordEditText;
    private CheckBox termsAndConditionCheckbox;
    private ImageButton passwordVisibleImageButton;
    private ImageButton retypePasswordVisibleImageButton;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // get firebase auth instance
        mAuth = FirebaseAuth.getInstance();

        // get reference to interactive components on the register activity
        signUpButton = findViewById(R.id.signUpButton);
        emailEditText = findViewById(R.id.emailEditText);
        nicknameEditText = findViewById(R.id.nicknameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        passwordVisibleImageButton = findViewById(R.id.passwordVisibleImageButton);
        retypePasswordEditText = findViewById(R.id.retypePasswordEditText);
        retypePasswordVisibleImageButton = findViewById(R.id.retypePasswordVisibleImageButton);
        termsAndConditionCheckbox = findViewById(R.id.termsAndConditionCheckbox);
        mainContentLayout = findViewById(R.id.mainContentLayout);

        // get a span by parsing out the HTML so that the string can be displayed as bold
        Spanned span = HtmlCompat.fromHtml(getString(R.string.userAlreadyHasAnAccount), HtmlCompat.FROM_HTML_MODE_LEGACY);
        SpannableString spannableString = new SpannableString(span);
        //  Color in the "Register" portion of the text in the spannable string with purple color
        spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.ordinaryButtonColor)), 25,spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        // set up the text view
        userAlreadyRegistered = new TextView(getApplicationContext());
        LinearLayout.LayoutParams mainContentParams = new LinearLayout.LayoutParams(mainContentLayout.getLayoutParams());
        mainContentParams.setMargins(0,40,0,0);
        userAlreadyRegistered.setLayoutParams(mainContentParams);
        userAlreadyRegistered.setClickable(true);
        userAlreadyRegistered.setGravity(Gravity.CENTER);
        userAlreadyRegistered.setMinWidth(48);
        userAlreadyRegistered.setText(spannableString);
        userAlreadyRegistered.setTextSize(16);
        mainContentLayout.addView(userAlreadyRegistered);

        // switch from login activity to the register activity when clicked on the "Don't have an account? Register" TextView on the login activity
        userAlreadyRegistered.setOnClickListener(view -> {
            Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(loginActivity);
        });

        signUpButton.setOnClickListener(view -> {
            createUserWithFirebase();
        });

        passwordVisibleImageButton.setOnClickListener(view ->{
            togglePasswordVisibilityButton(passwordVisibleImageButton, passwordEditText);
        });

        retypePasswordVisibleImageButton.setOnClickListener(view ->{
            togglePasswordVisibilityButton(retypePasswordVisibleImageButton, retypePasswordEditText);
        });

    }


    private boolean validatePassword(String password, String passwordRetyped) {
        StringBuilder errorMessagePassword = new StringBuilder();

        // check if passwords are empty
        if(password.isEmpty() || passwordRetyped.isEmpty()) {
            errorMessagePassword.append("* Password cannot be empty!\n");
        }

        // password length is less than 5 characters
        if(password.length() < 5) {
            errorMessagePassword.append("* Password needs to have at least 5 or more characters!\n");
        }

        // check if password contains at least 1 digit from (0 - 9)
        if(!password.matches(".*\\d+.*")) {
            errorMessagePassword.append("* Specify at least 1 numeric character (0-9)!\n");
        }

        // check if password contains at least 1 digits from special characters
        if(!password.matches(".*[$&+,:;=?@#|'<>.^*()%!-]+.*")) {
            errorMessagePassword.append("* Specify at least 1 special character ($&+,:;=?@#|'<>.^*()%!-)!\n");
        }

        // check if password contains at least 1 lowercase character
        if(!password.matches(".*[a-z]+.*")) {
            errorMessagePassword.append("* Specify at least 1 lowercase character (a-z)!\n");
        }

        // check if password contains at least 1 uppercase character
        if(!password.matches(".*[A-Z]+.*")) {
            errorMessagePassword.append("* Specify at least 1 1 uppercase letter (A-Z)!\n");
        }

        // passwords do not match and both are not empty
        if(!(password.equals(passwordRetyped)) && !(password.isEmpty() && passwordRetyped.isEmpty())) {
            errorMessagePassword.append("* Passwords do not match!");
        }

        // if no error messages were appended to the StringBuilder then all tests have passed and true is returned
        if(errorMessagePassword.length() != 0) {
            passwordEditText.setError(errorMessagePassword.toString());
            errorMessagePassword.setLength(0);
            return false;
        }

        return true;
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


    private boolean verifyCheckBox() {
        if(termsAndConditionCheckbox.isChecked()) {
            return true;
        } else {
            Toast.makeText(RegisterActivity.this, "Accept our terms and conditions!", Toast.LENGTH_LONG).show();
            return false;
        }
    }


    private void createUserWithFirebase() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String passwordReTyped = retypePasswordEditText.getText().toString();

        // check for email correctness
        boolean emailValidated = validateEmail(email);
        // check for password correctness
        boolean passwordValidated = validatePassword(password, passwordReTyped);
        // check if checkbox is ticked
        boolean checkBoxTicked = verifyCheckBox();


        if(emailValidated && passwordValidated && checkBoxTicked) {
            // add a listener which is triggered once the registration process is complete
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // display a toast informing that registration was successful
                                Toast.makeText(RegisterActivity.this, "Account successfully created!", Toast.LENGTH_LONG).show();
                                Intent mainPageActivity = new Intent(getApplicationContext(), MainPageActivity.class);
                                startActivity(mainPageActivity);
                            }
                        }
                    });
            // add an listener which should be triggered if something went wrong with the registration process, for example, a user with this email already exists
            mAuth.createUserWithEmailAndPassword(email, password).addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    createErrorAlertDialog("Authentication error!", e.getMessage());
                }
            });

            }

    }


    // utility method to an error alert dialog programmatically
    private void createErrorAlertDialog(String alertTitle, String alertMessage) {
        AlertDialog.Builder  alert = new AlertDialog.Builder(this);
        alert.setTitle(alertTitle);
        alert.setMessage(alertMessage);
        alert.setIcon(R.drawable.baseline_error_outline_24);
        alert.show();
    }

    private void togglePasswordVisibilityButton(ImageButton imgBtn, EditText editTextPass) {
        /* switch between the active and inactive state as defined in the ic_password_visible_toggle_button.xml file
            this will switch the image of the button and will set the new transformation method of the EditText
            if null, no transformation method is specified and the password appears as plaintext on the user screen
            otherwise set a new password transformation method which makes the password appear as sequence of dots */
        if (imgBtn.isActivated()) {
            imgBtn.setActivated(false);
            editTextPass.setTransformationMethod(null);
            editTextPass.setTransformationMethod(new PasswordTransformationMethod());

        } else {
            imgBtn.setActivated(true);
            editTextPass.setTransformationMethod(null);
        }

    }
}
