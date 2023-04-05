package com.example.fishcenter;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.ArrayList;

public class CreatePost extends AppCompatActivity implements PostsCallback {

    private ImageView userImageView;
    private boolean mediaSelected;

    private EditText postEditText;
    private EditText postTitleEditText;

    private VideoView userVideoView;
    private MediaController mediaController;
    private Uri userMediaUri;
    private PostsController postsController;
    private User currentUser;
    private String mimeType;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);
        userImageView = findViewById(R.id.userImageView);
        postEditText = findViewById(R.id.postEditText);
        mediaController = new MediaController(this);
        userVideoView = findViewById(R.id.userVideoView);
        postTitleEditText = findViewById(R.id.postTitleEditText);

        postsController = new PostsController(CreatePost.this, this);

        currentUser = (User) getIntent().getExtras().getSerializable("currentUser");


        final ImageButton goBackImageButton = findViewById(R.id.goBackImageButton);
        goBackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        final ImageButton sendPostImageButton = findViewById(R.id.sendPostImageButton);
        sendPostImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get post without whitespace at start and end
                String postBody = postEditText.getText().toString().trim();
                String postTitle = postTitleEditText.getText().toString().trim();
                boolean postValidated = postValidated(postBody);
                boolean postTitleValidated = postTitleValidated(postTitle);
                // post needs to have body and title other media is optional
                if (postValidated && postTitleValidated) {
                    if(userMediaUri != null) {
                        int flag = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(userMediaUri, flag);
                    }
                    postsController.savePostInBackend(postBody, postTitle, currentUser, mediaSelected, mimeType, userMediaUri);
                }
            }
        });


        final LinearLayout linearLayoutBackground = findViewById(R.id.linearLayoutBackground);
        final InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        linearLayoutBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                keyboard.hideSoftInputFromWindow(view.getWindowToken(),0);
                if(postEditText.isFocused()) {
                    postEditText.clearFocus();
                }
                if(postTitleEditText.isFocused()) {
                    postTitleEditText.clearFocus();
                }
                if(mediaController.isShown()) {
                    mediaController.hide();
                }

            }
        });


        final ActivityResultLauncher<PickVisualMediaRequest> pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if(uri != null) {
                userImageView.setVisibility(View.GONE);
                userVideoView.setVisibility(View.GONE);
                String mimeType = MediaUtilities.extractMediaMimeType(uri, getContentResolver());
                if(MediaUtilities.supportedImageMimeTypes.contains(mimeType)) {
                    // load the image from the uri into the image view using glide
                    // Glide helps round off the corners and load media into the image view
                    Glide.with(getApplicationContext()).load(uri).transform(new RoundedCorners(10)).into(userImageView);
                    userImageView.setVisibility(View.VISIBLE);
                } else if (MediaUtilities.supportedVideoMimeTypes.contains(mimeType)) {
                    userVideoView.setVisibility(View.VISIBLE);
                    userVideoView.setVideoURI(uri);
                    mediaController.setAnchorView(userVideoView);
                    userVideoView.setMediaController(mediaController);
                } else {
                    Toast.makeText(this, "Unsupported file format!", Toast.LENGTH_SHORT).show();
                    return;
                }
                mediaSelected = true;
                this.userMediaUri = uri;
                this.mimeType = mimeType;
            } else {
                mediaSelected = false;
                this.userMediaUri = null;
                this.mimeType = null;
            }
        });

        final ImageView selectImage = findViewById(R.id.selectImage);
        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPhotoPicker(pickMedia);
            }
        });


        // listen for the video to be loaded and ready to play
        userVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // if user video is present start and stop it right after to avoid the long black screen until started manually
                if(userVideoView != null) {
                    // make the video back to transparent
                    userVideoView.start();
                    userVideoView.pause();
                }
            }
        });


        userVideoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(userVideoView.isShown()) {
                    if(userVideoView.isPlaying()) {
                        userVideoView.pause();
                    } else {
                        userVideoView.start();
                    }
                }
            }
        });
    }

    private void returnNewPostToMainActivity(LocalPost localPost) {
        Intent mainPage = new Intent();
        setResult(Activity.RESULT_OK, mainPage);
        mainPage.putExtra("localPost", localPost);
        finish();
    }

    private boolean postValidated(String post) {
        if (post.length() == 0) {
            postEditText.setError("Post body cannot be empty!");
            return false;
        }
        return true;
    }

    private boolean postTitleValidated(String postTitle) {
        if (postTitle.length() == 0) {
            postTitleEditText.setError("Post title cannot be empty!");
            return false;
        }
        return true;
    }


    private void launchPhotoPicker(ActivityResultLauncher<PickVisualMediaRequest> pickMedia) {
        // Launch the photo picker and allow the user to choose only images.
        // although the compiler complains about passing an invalid type to the setMediaType method the Photo Picker works fine and the application runs without crashing
        pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE).build());
    }


    @Override
    public void userPostsReady(ArrayList<PostModel> userPosts) {
        // nothing to do here
    }

    @Override
    public void userPostUpdated(int position) {
        // nothing to do here
    }

    @Override
    public void newPostSaved(LocalPost newPost) {
        returnNewPostToMainActivity(newPost);
    }

    @Override
    public void isSynchronisationNecessary(boolean isNecessary) {
        // nothing to do here
    }

}