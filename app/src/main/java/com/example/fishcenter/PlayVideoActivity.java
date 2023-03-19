package com.example.fishcenter;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class PlayVideoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_video);
        Uri videoUri = Uri.parse(getIntent().getExtras().getString("video"));
        VideoView videoView = findViewById(R.id.videoView);
        MediaController mediaController = new MediaController(PlayVideoActivity.this);
        videoView.setVideoURI(videoUri);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);
        ImageButton goBackImageButton = findViewById(R.id.goBackImageButton);
        ImageButton logoutImageButton = findViewById(R.id.logoutImageButton);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        LinearLayout videoTimeoutSpinner = findViewById(R.id.videoTimeoutSpinner);
        videoTimeoutSpinner.setVisibility(View.VISIBLE);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                videoTimeoutSpinner.setVisibility(View.INVISIBLE);
                videoView.start();
            }
        });

        goBackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        logoutImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createLogoutDialog(firebaseAuth);
            }
        });
    }

    public void createLogoutDialog(FirebaseAuth firebaseAuthInstance) {
        AlertDialog.Builder logoutDialog = new AlertDialog.Builder(PlayVideoActivity.this);
        logoutDialog.setMessage("Are you sure you want to sign out?");
        logoutDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                firebaseAuthInstance.signOut();
                Intent goBackToLogin = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(goBackToLogin);
                finish();
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
}
