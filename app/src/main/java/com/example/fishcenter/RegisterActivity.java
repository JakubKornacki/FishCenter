package com.example.fishcenter;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;


public class RegisterActivity extends AppCompatActivity {

    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private final FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private EditText emailEditText;
    private EditText nicknameEditText;
    private EditText passwordEditText;
    private EditText retypePasswordEditText;
    private CheckBox termsAndConditionCheckbox;
    private ImageButton passwordVisibleImageButton;
    private ImageButton retypePasswordVisibleImageButton;
    private LinearLayout progressSpinnerLayout;
    private LinearLayout mainContentLayout;
    private ImageButton registerButton;
    private LinearLayout backToLoginLayout;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // get reference to interactive components on the register activity
        emailEditText = findViewById(R.id.emailEditText);
        nicknameEditText = findViewById(R.id.nicknameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        retypePasswordVisibleImageButton = findViewById(R.id.retypePasswordVisibleImageButton);
        termsAndConditionCheckbox = findViewById(R.id.termsAndConditionCheckbox);

        progressSpinnerLayout = findViewById(R.id.progressSpinnerLayout);

        mainContentLayout = findViewById(R.id.mainContentLayout);
        backToLoginLayout = createBackToLoginLayout(mainContentLayout);
        mainContentLayout.addView(backToLoginLayout);

        registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(view -> createUserWithFirebase());

        passwordVisibleImageButton = findViewById(R.id.passwordVisibleImageButton);
        passwordVisibleImageButton.setOnClickListener(view -> togglePasswordVisibilityButton(passwordVisibleImageButton, passwordEditText));

        retypePasswordEditText = findViewById(R.id.retypePasswordEditText);
        retypePasswordVisibleImageButton.setOnClickListener(view -> togglePasswordVisibilityButton(retypePasswordVisibleImageButton, retypePasswordEditText));


        // if the user click anywhere on the background linear layout which is anywhere on the screen apart from the top bar
        // the focus from the edit texts should be cleared and the input keyboard should be hidden
        final LinearLayout linearLayoutBackground = findViewById(R.id.linearLayoutBackground);
        linearLayoutBackground.setOnClickListener(view -> {
            if(emailEditText.isFocused()) {
                emailEditText.clearFocus();
            } else if (passwordEditText.isFocused()) {
                passwordEditText.clearFocus();
            } else if (passwordEditText.isFocused()) {
                passwordEditText.clearFocus();
            } else if(retypePasswordEditText.isFocused()) {
                retypePasswordEditText.clearFocus();
            }
            // get the input keyboard and hide soft hide input keyboard from the window
            // https://stackoverflow.com/questions/1109022/how-to-close-hide-the-android-soft-keyboard-programmatically/15587937#15587937
            InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            // hide the keyboard
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
        final TextView userAlreadyRegistered = new TextView(getApplicationContext());
        userAlreadyRegistered.setLayoutParams(textLinearLayoutParams);
        // define the text view appearance and position
        userAlreadyRegistered.setClickable(true);
        userAlreadyRegistered.setMinWidth(48);
        userAlreadyRegistered.setTextSize(16);
        userAlreadyRegistered.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_10_gray_opacity_30_to_transparent));
        textLinearLayout.addView(userAlreadyRegistered);
        // get the spannable string
        SpannableString text = createSpannableString();
        userAlreadyRegistered.setText(text);

        // switch from login activity to the register activity when clicked on the "Don't have an account? Register" TextView on the login activity
        userAlreadyRegistered.setOnClickListener(view -> {
            finish();
        });

        return textLinearLayout;
    }

    private SpannableString createSpannableString() {
        // get a span by parsing out the HTML so that the string can be displayed as bold
        Spanned span = HtmlCompat.fromHtml(getString(R.string.userAlreadyHasAnAccount), HtmlCompat.FROM_HTML_MODE_LEGACY);
        SpannableString spannableString = new SpannableString(span);
        //  Color in the "Register" portion of the text in the spannable string with purple color
        spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.black)), 0,25, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.ordinaryButtonColor)), 25,spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }



    private boolean validatePassword(String password, String passwordRetyped) {
        StringBuilder errorMessagePassword = new StringBuilder();

        // check if passwords are empty
        if(password.isEmpty() || passwordRetyped.isEmpty()) {
            errorMessagePassword.append("Passwords cannot be empty!\n");
        }

        // password length is less than 5 characters
        if(password.length() < 5) {
            errorMessagePassword.append("Password needs to have at least 5 or more characters!\n");
        }

        // check if password contains at least 1 digit from (0 - 9)
        if(!password.matches(".*\\d+.*")) {
            errorMessagePassword.append("Specify at least 1 numeric character (0-9)!\n");
        }

        // check if password contains at least 1 digits from special characters
        if(!password.matches(".*[$&+,:;=?@#|'<>.^*()%!-]+.*")) {
            errorMessagePassword.append("Specify at least 1 special character ($&+,:;=?@#|'<>.^*()%!-)!\n");
        }

        // check if password contains at least 1 lowercase character
        if(!password.matches(".*[a-z]+.*")) {
            errorMessagePassword.append("Specify at least 1 lowercase character (a-z)!\n");
        }

        // check if password contains at least 1 uppercase character
        if(!password.matches(".*[A-Z]+.*")) {
            errorMessagePassword.append("Specify at least 1 1 uppercase letter (A-Z)!\n");
        }

        // passwords do not match and both are not empty
        if(!(password.equals(passwordRetyped)) && !(password.isEmpty() && passwordRetyped.isEmpty())) {
            retypePasswordEditText.setError("Passwords need to match!");
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

    private boolean validateNickname(String nickname) {
        StringBuilder errorMessage = new StringBuilder();
        if(nickname.length() < 5) {
            errorMessage.append("Password needs to have at least 5 or more characters!\n");
        }

        if(nickname.contains(" ")){
            errorMessage.append("Nickname cannot contain spaces!");
        }
        if(errorMessage.length() != 0) {
            nicknameEditText.setError(errorMessage);
            return false;
        }

        return true;
    }


    private boolean validateCheckBox() {
        if(termsAndConditionCheckbox.isChecked()) {
            return true;
        } else {
            Toast.makeText(RegisterActivity.this, "Accept terms and conditions!", Toast.LENGTH_LONG).show();
            return false;
        }
    }


    private void createUserWithFirebase() {
        String email = emailEditText.getText().toString().trim();
        String nickname = nicknameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String passwordReTyped = retypePasswordEditText.getText().toString();
        // check for email correctness
        boolean emailValidated = validateEmail(email);
        // check if nickname is valid
        boolean nicknameValidated = validateNickname(nickname);
        // check for password correctness
        boolean passwordValidated = validatePassword(password, passwordReTyped);
        // check if checkbox is ticked
        boolean checkBoxTicked = validateCheckBox();

        if(emailValidated && nicknameValidated && passwordValidated && checkBoxTicked) {
            showSpinnerAndClearFocus(true);
            // add a listener which is triggered once the registration process is complete
            firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // create a userid-nickname pair in FireStore
                                // current user ID should exist now
                                String currentUserId =  firebaseAuth.getCurrentUser().getUid();
                                createUserIDNicknamePairFireStore(currentUserId, nickname);
                            }
                        }
                    });
            // add an listener which should be triggered if something went wrong with the registration process, for example, a user with this email already exists
            firebaseAuth.createUserWithEmailAndPassword(email, password).addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    createErrorAlertDialog("Authentication error:", e.getMessage());
                    showSpinnerAndClearFocus(false);
                }
            });
        }
    }

    private void createUserIDNicknamePairFireStore(String currentUserId, String nickName) {
        Map<String, Object> nickname = new HashMap<>();
        nickname.put("nickname", nickName);
        firebaseFirestore.collection("users").document(currentUserId).set(nickname).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Uri profilePicture = getDefaultProfilePicture();
                StorageReference storageRef = firebaseStorage.getReference();
                storageRef.child("/profilePictures/" + currentUserId + "/").putFile(profilePicture);
                // redirect user to the main page
                Intent mainPageActivity = new Intent(getApplicationContext(), MainPageActivity.class);
                startActivity(mainPageActivity);
                showSpinnerAndClearFocus(false);
            }
        });
    }

    private Uri getDefaultProfilePicture() {
        int profilePicId = R.mipmap.img_profile_pic_white_default_round;
        Uri defaultProfilePicture = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getResources().getResourcePackageName(profilePicId) + "/" + getResources().getResourceTypeName(profilePicId) + "/" + getResources().getResourceEntryName(profilePicId));
        return defaultProfilePicture;
    }


    // utility method to an error alert dialog programmatically
    private void createErrorAlertDialog(String alertTitle, String alertMessage) {
        AlertDialog.Builder  alert = new AlertDialog.Builder(this);
        alert.setTitle(alertTitle);
        alert.setMessage(alertMessage);
        alert.setIcon(R.drawable.ic_baseline_error_outline_36_black);
        alert.show();
    }

    private void togglePasswordVisibilityButton(ImageButton imgBtn, EditText editTextPass) {
        // switch between the active and inactive state as defined in the ic_password_visible_toggle_button.xml file
        // this will switch the image of the button and will set the new transformation method of the EditText
        // if null, no transformation method is specified and the password appears as plaintext on the user screen
        // otherwise set a new password transformation method which makes the password appear as sequence of dots
        if (imgBtn.isActivated()) {
            imgBtn.setActivated(false);
            editTextPass.setTransformationMethod(null);
            editTextPass.setTransformationMethod(new PasswordTransformationMethod());

        } else {
            imgBtn.setActivated(true);
            editTextPass.setTransformationMethod(null);
        }
        // set text pointer to the position at the end of text changing the transformation method sets it back to zero
        int textLen = editTextPass.getText().length();
        editTextPass.setSelection(textLen);
    }

    private void showSpinnerAndClearFocus(boolean flag) {
        emailEditText.setFocusable(!flag);
        nicknameEditText.setFocusable(!flag);
        passwordEditText.setFocusable(!flag);
        retypePasswordEditText.setFocusable(!flag);
        termsAndConditionCheckbox.setClickable(!flag);
        registerButton.setClickable(!flag);
        backToLoginLayout.getChildAt(0).setClickable(!flag);
        if(flag) {
            progressSpinnerLayout.setVisibility(View.VISIBLE);
            registerButton.setBackground(null);
            backToLoginLayout.getChildAt(0).setBackground(null);
        } else {
            progressSpinnerLayout.setVisibility(View.INVISIBLE);
            registerButton.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_30_to_transparent));
            backToLoginLayout.getChildAt(0).setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_30_to_transparent));
        }
    }

}
