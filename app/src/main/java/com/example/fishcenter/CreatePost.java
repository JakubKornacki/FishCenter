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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import pl.droidsonroids.gif.GifImageView;

public class CreatePost extends AppCompatActivity {

    private ImageView userImageView;
   private boolean mediaSelected;

   private EditText postEditText;
   private EditText postTitleEditText;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
   private InputMethodManager keyboard;
    private GifImageView userGifImageView;
   private VideoView userVideoView;
   private MediaController mediaController;
   private FirebaseStorage firebaseStorage;
   private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
   private FirebaseFirestore firestore;

   private Uri uri;
   private final HashSet<String> imageMimeTypes = new HashSet<>(Arrays.asList("image/jpg","image/jpeg","image/png"));
   private final HashSet<String> videoMimeTypes = new HashSet<>(Arrays.asList("video/3gp","video/mov","video/avi","video/wmv","video/mp4","video/mpeg"));
   private String postBody;
   private String postTitle;
   private byte[] userProfilePicture;
   private final String currentUserId = firebaseAuth.getCurrentUser().getUid();

   private String userNickname;
   private String mimeType;
   private PostModel newPost;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);
        LinearLayout linearLayoutBackground = findViewById(R.id.linearLayoutBackground);
        ImageView selectImage = findViewById(R.id.selectImage);
        userImageView = findViewById(R.id.userImageView);
        postEditText = findViewById(R.id.postEditText);
        ImageButton goBackImageButton = findViewById(R.id.goBackImageButton);
        ImageButton sendPostImageButton = findViewById(R.id.sendPostImageButton);
        mediaController = new MediaController(this);
        userGifImageView = findViewById(R.id.postGif);
        userVideoView = findViewById(R.id.userVideoView);
        postTitleEditText = findViewById(R.id.postTitleEditText);
        firebaseStorage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        // extract the user profile picture passed in from the main activity used to create the new post
        userProfilePicture = (byte[]) getIntent().getExtras().getSerializable("profilePicture");
        userNickname = getIntent().getExtras().getString("userNickname");
        goBackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               finish();
            }
        });

        sendPostImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get post without whitespace at start and end
                postBody = postEditText.getText().toString().trim();
                postTitle = postTitleEditText.getText().toString().trim();
                boolean postValidated = postValidated(postBody);
                boolean postTitleValidated = postTitleValidated(postTitle);
                // post needs to have body and title other media is optional
                if (postValidated && postTitleValidated) {
                    savePostInBackend();
                }
            }
        });


        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPhotoPicker();
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


        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if(uri != null) {
                String mimeType = extractFileMimeType(uri);
                userImageView.setVisibility(View.GONE);
                userGifImageView.setVisibility(View.GONE);
                userVideoView.setVisibility(View.GONE);
                this.uri = null;
                this.mimeType = null;
                if(imageMimeTypes.contains(mimeType)) {
                    // load the image from the uri into the image view using glide
                    // Glide helps round off the corners and load the gif from the uri into the GifImageView and ensuring it is not bigger than 360x360 pixels
                    Glide.with(getApplicationContext()).load(uri).transform(new RoundedCorners(10)).into(userImageView);
                    userImageView.setVisibility(View.VISIBLE);
                } else if (videoMimeTypes.contains(mimeType)) {
                    userVideoView.setVisibility(View.VISIBLE);
                    userVideoView.setVideoURI(uri);
                    mediaController.setAnchorView(userVideoView);
                    userVideoView.setMediaController(mediaController);
                } else if(mimeType.equals("image/gif")){
                    // https://github.com/koral--/android-gif-drawable
                    // use Glide along with Droids On Roids library for displaying animated gifs in custom ImageViews which animate gifs
                    // if the gif is not animated it will behave as an ordinary image view.
                    // Glide helps round off the corners and load the gif from the uri into the GifImageView and ensuring it is not bigger than 360x360 pixels
                    Glide.with(getApplicationContext()).asGif().load(uri).transform(new RoundedCorners(10)).into(userGifImageView);
                    userGifImageView.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(this, "Unsupported file format!", Toast.LENGTH_SHORT).show();
                    return;
                }
                this.uri = uri;
                this.mimeType = extractFileMimeType(uri);
                mediaSelected = true;
            } else {
                mediaSelected = false;
            }
        });

    }

    private void startMainActivity(LocalPost localPost) {
        Intent mainPage = new Intent();
        setResult(Activity.RESULT_OK, mainPage);
        mainPage.putExtra("localPost", localPost);
        finish();
    }


    private String extractFileMimeType(Uri file) {
        if(file == null) {
            return null;
        }
        return getContentResolver().getType(file);
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

    private void savePostInBackend() {
        // mark the timestamp at the beginning of creating a post
        Map<String, Object> post = new HashMap<>();
        Timestamp timestamp = Timestamp.now();
        DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        String postUploadDate = dateFormatter.format(timestamp.toDate());
        String mimeType = null;
        if(uri != null) {
            mimeType = extractFileMimeType(uri);
        }
        post.put("title", postTitle);
        post.put("body", postBody);
        post.put("timestamp", timestamp);
        post.put("likes", 0);
        post.put("userId", currentUserId);
        post.put("nickname", userNickname);
        post.put("mimeType", mimeType);
        firestore.collection("posts").add(post).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                String uniquePostRef = documentReference.getId();
                String mimeType = null;
                if(uri != null) {
                    mimeType = extractFileMimeType(uri);
                }
                //  public PostModel(@NonNull Context context, String title, String body, byte[] profilePhoto, String nickname, String postUploadDate, String numLikes, String media, String mimeType, String uniquePostRef, String userId, ArrayList<String> postLikedBy) {
                //
                // update firebase storage
                if(mediaSelected) {
                    StorageReference storageRef = firebaseStorage.getReference();
                    storageRef.child("/postMedia/" + uniquePostRef + "/").putFile(uri);
                }
                // put in the unique post to firestore
                post.put("postId", uniquePostRef);
                firestore.collection("posts").document(uniquePostRef).update(post);
                LocalPost localPost = new LocalPost(postTitle, postBody, userProfilePicture, userNickname, postUploadDate, "0", String.valueOf(uri), mimeType, uniquePostRef, currentUserId, null);
                startMainActivity(localPost);
            }
        });
    }

    private void launchPhotoPicker () {
        // Launch the photo picker and allow the user to choose only images.
        // although the compiler complains about passing an invalid type to the setMediaType method the Photo Picker works fine and the application runs without crashing
        pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE).build());
    }

}
