package com.example.fishcenter;

import static android.content.ContentValues.TAG;
import com.google.firebase.auth.FirebaseAuth;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.core.FirestoreClient;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import pl.droidsonroids.gif.GifImageView;

public class CreatePost extends AppCompatActivity {

   private ImageView selectImage;
   private ImageView userImageView;
   private boolean mediaSelected;

   private EditText postEditText;
   private EditText postTitleEditText;
   private LinearLayout linearLayoutBackground;
   private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
   private InputMethodManager keyboard;
   private ImageButton goBackImageButton;
   private ImageButton sendPostImageButton;
   private GifImageView userGifImageView;
   private VideoView userVideoView;
   private MediaController mediaController;
   private FirebaseStorage firebaseStorage;
   private FirebaseAuth firebaseAuth;
   private FirebaseFirestore fireStore;

   private Uri uri;
   private HashSet<String> imageMimeTypes = new HashSet<>(Arrays.asList("image/jpg","image/jpeg","image/png"));
   private HashSet<String> videoMimeTypes = new HashSet<>(Arrays.asList("video/3gp","video/mov","video/avi","video/wmv","video/mp4","video/mpeg"));
   private String postBody;
   private String postTitle;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);
        linearLayoutBackground = findViewById(R.id.linearLayoutBackground);
        selectImage = findViewById(R.id.selectImage);
        userImageView = findViewById(R.id.userImageView);
        postEditText = findViewById(R.id.postEditText);
        goBackImageButton = findViewById(R.id.goBackImageButton);
        sendPostImageButton = findViewById(R.id.sendPostImageButton);
        mediaController = new MediaController(this);
        userGifImageView = findViewById(R.id.userGifImageView);
        userVideoView = findViewById(R.id.userVideoView);
        postTitleEditText = findViewById(R.id.postTitleEditText);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        fireStore = FirebaseFirestore.getInstance();
        keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);


        goBackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mainActivity = new Intent(getApplicationContext(), MainPageActivity.class);
                startActivity(mainActivity);
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
                    createPost();
                    // go back to main page
                    Intent mainPage = new Intent(getApplicationContext(), MainPageActivity.class);
                    startActivity(mainPage);
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
                if(postEditText.isFocused()) {
                    postEditText.clearFocus();
                    keyboard.hideSoftInputFromWindow(view.getWindowToken(),0);
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
                if(imageMimeTypes.contains(mimeType)) {
                    // load the image from the uri into the image view using glide
                    // Glide helps round off the corners and load the gif from the uri into the GifImageView and ensuring it is not bigger than 360x360 pixels
                    Glide.with(getApplicationContext()).load(uri).transform(new RoundedCorners(10)).into(userImageView);
                    userImageView.setVisibility(View.VISIBLE);
                } else if (videoMimeTypes.contains(mimeType)) {
                    userVideoView.setVisibility(View.VISIBLE);
                    userVideoView.setVideoURI(uri);
                    mediaController.setAnchorView(userImageView);
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
                mediaSelected = true;
            } else {
                mediaSelected = false;
            }
        });

    }


    private String extractFileMimeType(Uri file) {
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


    private void createPost() {

        // need to get user post number so that relationship between cloud storage and firstore for posts can be maintained
        // will be used as the file name in cloud storage in path /postMedia/userID/1 ... n



       fireStore.collection("user-id-nickname-pair").document(firebaseAuth.getUid()).collection("posts").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                int numUserPosts = task.getResult().size();
                numUserPosts++;
                // save media if selected
                if(mediaSelected) {
                    StorageReference storageRef = firebaseStorage.getReference();
                    storageRef.child("/postMedia/" + firebaseAuth.getUid() + "/" + numUserPosts).putFile(uri);
                }
                // save post title and body despite having media

                // create
                Map<String, Object> post = new HashMap<>();
                Timestamp timestamp = Timestamp.now();
                post.put("title", postTitle);
                post.put("timestamp", timestamp);
                post.put("body", postBody);
                fireStore.collection("user-id-nickname-pair").document(firebaseAuth.getUid()).collection("posts").document(String.valueOf(numUserPosts)).set(post).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
           @Override
           public void onFailure(@NonNull Exception e) {
               //Log.e(TAG, e.getMessage());
           }
       });
    }

    private void launchPhotoPicker () {
        // Launch the photo picker and allow the user to choose only images.
        // although the compiler complains about passing an invalid type to the setMediaType method the Photo Picker works fine and the application runs without crashing
        pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE).build());
    }



}
