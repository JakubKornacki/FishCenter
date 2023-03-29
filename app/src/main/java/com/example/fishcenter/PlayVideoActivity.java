package com.example.fishcenter;

import android.content.DialogInterface;
import android.content.Intent;
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

import java.io.File;


public class PlayVideoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_video);

        VideoView videoView = findViewById(R.id.videoView);
        Uri videoUri = Uri.parse(getIntent().getExtras().getString("video"));
        videoView.setVideoURI(videoUri);
        MediaController mediaController = new MediaController(PlayVideoActivity.this);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);
        LinearLayout videoTimeoutSpinner = findViewById(R.id.videoTimeoutSpinner);
        videoTimeoutSpinner.setVisibility(View.VISIBLE);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                videoTimeoutSpinner.setVisibility(View.INVISIBLE);
                videoView.start();
            }
        });


        ImageButton goBackImageButton = findViewById(R.id.goBackImageButton);
        ImageButton logoutImageButton = findViewById(R.id.logoutImageButton);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        goBackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        // button for logging out of the application
        logoutImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertUtilities.createLogoutDialog(PlayVideoActivity.this, firebaseAuth);
            }
        });
    }

}
