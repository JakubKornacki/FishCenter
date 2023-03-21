package com.example.fishcenter;

import android.app.Activity;
import android.content.DialogInterface;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class CreatePost extends AppCompatActivity {

    private ImageView userImageView;
    private boolean mediaSelected;

    private EditText postEditText;
    private EditText postTitleEditText;
    private InputMethodManager keyboard;
    private VideoView userVideoView;
    private MediaController mediaController;
    private FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private final String currentUserId = firebaseAuth.getCurrentUser().getUid();
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private Uri userMediaUri;

    private byte[] userProfilePicture;
    private String userNickname;
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

        // extract the user profile picture passed in from the main activity used to create the new post
        userProfilePicture = (byte[]) getIntent().getExtras().getSerializable("profilePicture");
        userNickname = getIntent().getExtras().getString("userNickname");


        ImageButton goBackImageButton = findViewById(R.id.goBackImageButton);
        goBackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        ImageButton sendPostImageButton = findViewById(R.id.sendPostImageButton);
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
                    savePostInBackend(postBody, postTitle);
                }
            }
        });


        LinearLayout linearLayoutBackground = findViewById(R.id.linearLayoutBackground);
        InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
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
                    // Glide helps round off the corners and load the gif from the uri into the GifImageView and ensuring it is not bigger than 360x360 pixels
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
                this.userMediaUri = uri;
                this.mimeType = MediaUtilities.extractMediaMimeType(uri, getContentResolver());
                mediaSelected = true;
            } else {
                mediaSelected = false;
                this.userMediaUri = null;
                this.mimeType = null;
            }
        });

        ImageView selectImage = findViewById(R.id.selectImage);
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

    private void savePostInBackend(String postBody, String postTitle) {
        // mark the timestamp at the beginning of creating a post
        Map<String, Object> post = new HashMap<>();

        Timestamp timestamp = Timestamp.now();
        DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");

        post.put("title", postTitle);
        post.put("body", postBody);
        post.put("timestamp", timestamp);
        post.put("likes", 0);
        post.put("dislikes", 0);
        post.put("userId", currentUserId);
        post.put("nickname", userNickname);
        post.put("mimeType", mimeType);
        firestore.collection("posts").add(post).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                String uniquePostRef = documentReference.getId();
                String postUploadDate = dateFormatter.format(timestamp.toDate());

                // update firebase storage
                if(mediaSelected) {
                    StorageReference storageRef = firebaseStorage.getReference();
                    storageRef.child("/postMedia/" + uniquePostRef + "/").putFile(userMediaUri);
                }
                // put in the unique post to firestore
                post.put("postId", uniquePostRef);
                firestore.collection("posts").document(uniquePostRef).update(post);

                // create a local copy of the post
               LocalPost localPost = new LocalPost(postTitle, postBody, userProfilePicture, userNickname, postUploadDate, "0", "0", String.valueOf(userMediaUri), mimeType, uniquePostRef, currentUserId);
               returnNewPostToMainActivity(localPost);
            }
        });
    }

    private void launchPhotoPicker(ActivityResultLauncher<PickVisualMediaRequest> pickMedia) {
        // Launch the photo picker and allow the user to choose only images.
        // although the compiler complains about passing an invalid type to the setMediaType method the Photo Picker works fine and the application runs without crashing
        pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE).build());
    }

}