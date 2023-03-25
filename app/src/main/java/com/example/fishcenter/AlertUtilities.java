package com.example.fishcenter;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;

public class AlertUtilities {

    // utility method to an error sign-out "are you sure" message
    public static void createLogoutDialog(Context context, FirebaseAuth firebaseAuthInstance) {
        final AlertDialog.Builder logoutDialog = new AlertDialog.Builder(context);
        logoutDialog.setMessage("Are you sure you want to sign out?");
        logoutDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                firebaseAuthInstance.signOut();
                Intent goBackToLogin = new Intent(context, LoginActivity.class);
                startActivity(context, goBackToLogin, null);
                Activity activity = (Activity) context;
                activity.finish();
            }
        });

        logoutDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        logoutDialog.show();
    }

    // utility method to an error alert dialog programmatically
    public static void createErrorAlertDialog(Context context, String alertTitle, String alertMessage) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(alertTitle);
        alert.setMessage(alertMessage);
        alert.setIcon(R.drawable.ic_baseline_error_outline_36_black);
        alert.show();
    }

}
