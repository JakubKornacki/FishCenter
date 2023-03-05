package com.example.fishcenter;

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
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class PlayVideoActivity extends AppCompatActivity {
    private VideoView videoView;
    private MediaController mediaController;
    private ImageButton goBackImageButton;
    private ImageButton logoutImageButton;
    private FirebaseAuth firebaseAuth;
    private LinearLayout videoTimeoutSpinner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_video);
        Uri videoUri = Uri.parse(getIntent().getExtras().getString("video"));
        videoView = findViewById(R.id.videoView);
        mediaController = new MediaController(PlayVideoActivity.this);
        videoView.setVideoURI(videoUri);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);
        goBackImageButton = findViewById(R.id.goBackImageButton);
        logoutImageButton = findViewById(R.id.logoutImageButton);
        videoTimeoutSpinner = findViewById(R.id.videoTimeoutSpinner);
        firebaseAuth = FirebaseAuth.getInstance();
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
                firebaseAuth.signOut();
                Intent goBackToLogin = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(goBackToLogin);
                finish();
            }
        });



    }
}
