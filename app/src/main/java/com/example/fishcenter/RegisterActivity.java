package com.example.fishcenter;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.*;
import com.google.firebase.auth.*;


public class RegisterActivity extends AppCompatActivity {

    // firebase authentication object instance
    private FirebaseAuth mAuth;
    private TextView switchToLoginActivityRegisterActivity;
    private Button buttonRegisterActivity;
    private EditText editTextEmailRegisterActivity;
    private EditText editTextPasswordRegisterActivity;
    private EditText editTextReTypePasswordRegisterActivity;
    private CheckBox termsAndConditionsCheckBoxRegisterActivity;
    private ImageButton passwordVisibleImageButtonRegisterActivity;
    private ImageButton reTypePasswordVisibleImageButtonRegisterActivity;




    // do i really need this?
    @Override
    public void onStart() {
        super.onStart();


    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // get reference to interactive components on the register activity
        getReferencesOfComponents();
        setupOnClickHandlers();

    }

    @NonNull
    private boolean verifyPassword(String password, String passwordRetyped) {
        StringBuilder errorMessagePassword = new StringBuilder();

        // check if passwords are empty
        if(password.isEmpty() || passwordRetyped.isEmpty()) {
            errorMessagePassword.append("* Password cannot be empty!\n");
        }


        // password length is less than 5 characters
        if(password.length() < 5) {
            errorMessagePassword.append("* Password needs to have 5 or more characters!\n");
        }

        // check if password contains at least 1 digit from (0 - 9)
        // check if password contains at least 1 digits from special characters
        // check if password contains at least 1 lowercase character
        // check if password contains at least 1 uppercase character
        if(!(password.matches(".*[0-9]+.*") && password.matches(".*[$&+,:;=?@#|'<>.^*()%!-]+.*") && password.matches(".*[a-z]+.*") && password.matches(".*[A-Z]+.*"))) {
            errorMessagePassword.append("* Password needs at least:\n 1 numeric character (0-9)!\n 1 special character ($&+,:;=)%!-) etc.\n 1 lowercase character (a-z)\n 1 uppercase letter (A-Z)");
        }



        // passwords do not match and both are not empty
        if(!(password.equals(passwordRetyped)) && !(password.isEmpty() && passwordRetyped.isEmpty())) {
            errorMessagePassword.append("* Passwords need to match!");
        }

        // if no error messages were appended to the StringBuilder then all tests have passed and true is returned
        if(errorMessagePassword.length() != 0) {
            editTextPasswordRegisterActivity.setError(errorMessagePassword.toString());
            errorMessagePassword.setLength(0);
            return false;
        }

        return true;
    }

    private boolean verifyEmail(String email) {
        StringBuilder errorMessageEmail = new StringBuilder();

        if(email.isEmpty()) {
            errorMessageEmail.append("* E-mail cannot be empty!\n");
        }

        // email length exceeded
        if(email.length() > 320) {
            errorMessageEmail.append("* E-mail length cannot exceed 320 characters!\n");
        }

        // make sure email contains '.' and '@'
        if((email.indexOf('.') == -1 ) && email.indexOf('@') == -1 ) {
            errorMessageEmail.append("* E-mail needs to contains '.' and '@'!");
        }
        if(errorMessageEmail.length() != 0) {
            editTextEmailRegisterActivity.setError(errorMessageEmail.toString());
            errorMessageEmail.setLength(0);
            return false;
        }

        return true;
    }


    private boolean verifyCheckBox() {
        if(termsAndConditionsCheckBoxRegisterActivity.isChecked()) {
            return true;
        } else {
            Toast.makeText(RegisterActivity.this, "Please accept our terms and conditions!", Toast.LENGTH_LONG).show();
            return false;
        }
    }


    private void createUserWithFirebase() {
        String email = editTextEmailRegisterActivity.getText().toString();
        String password = editTextPasswordRegisterActivity.getText().toString();
        String passwordReTyped = editTextReTypePasswordRegisterActivity.getText().toString();

        // check for email correctness
        boolean emailVerified = verifyEmail(email);
        // check for password correctness
        boolean passwordVerified = verifyPassword(password, passwordReTyped);
        // check if checkbox is ticked
        boolean checkBoxTicked = verifyCheckBox();


        if(emailVerified && passwordVerified && checkBoxTicked) {
            // add a listener which is triggered once the registration process is complete
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // display a toast informing that registration was successful
                                Toast.makeText(RegisterActivity.this, "Account successfully created!", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
            // add an listener which should be triggered if something went wrong with the registration process, for example, a user with this email already exists
            mAuth.createUserWithEmailAndPassword(email, password).addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            }

    }


    private void setupOnClickHandlers() {
        // switch from login activity to the register activity when clicked on the "Don't have an account? Register" TextView on the login activity
        switchToLoginActivityRegisterActivity.setOnClickListener(view -> {
            Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(loginActivity);
        });

        buttonRegisterActivity.setOnClickListener(view -> {
            createUserWithFirebase();
        });

        passwordVisibleImageButtonRegisterActivity.setOnClickListener(view ->{
            togglePasswordVisibilityButton(passwordVisibleImageButtonRegisterActivity, editTextPasswordRegisterActivity);
        });

        reTypePasswordVisibleImageButtonRegisterActivity.setOnClickListener(view ->{
            togglePasswordVisibilityButton(reTypePasswordVisibleImageButtonRegisterActivity, editTextReTypePasswordRegisterActivity);
        });

    }


    private void getReferencesOfComponents() {
        switchToLoginActivityRegisterActivity = findViewById(R.id.switchToLoginActivityRegisterActivity);
        buttonRegisterActivity = findViewById(R.id.buttonRegisterActivity);
        editTextEmailRegisterActivity = findViewById(R.id.editTextEmailRegisterActivity);
        editTextPasswordRegisterActivity = findViewById(R.id.editTextPasswordRegisterActivity);
        editTextReTypePasswordRegisterActivity = findViewById(R.id.editTextReTypePasswordRegisterActivity);
        termsAndConditionsCheckBoxRegisterActivity = findViewById(R.id.termsAndConditionsCheckBoxRegisterActivity);
        passwordVisibleImageButtonRegisterActivity = findViewById(R.id.passwordVisibleImageButtonRegisterActivity);
        reTypePasswordVisibleImageButtonRegisterActivity = findViewById(R.id.reTypePasswordVisibleImageButtonRegisterActivity);

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
