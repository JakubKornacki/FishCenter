package com.example.fishcenter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailEditText;
    private Button submitButton;
   // private ImageButton goBackButton;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        emailEditText = findViewById(R.id.emailEditText);
        submitButton = findViewById(R.id.submitButton);
       // goBackButton = findViewById(R.id.goBackButton);
        auth = FirebaseAuth.getInstance();
        submitButton.setOnClickListener(view -> resetPasswordWithFirebase());
        /*
        goBackButton.setOnClickListener(view -> {
            Intent backToLogin = new Intent(this, LoginActivity.class);
            startActivity(backToLogin);
        }); */
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


}
