package com.example.fishcenter;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainPageActivity extends AppCompatActivity implements OnClickListener {

    private ImageButton fishRecognitionImageButton;
    private ImageButton googleMapsButton;
    private ImageButton logoutImageButton;
    private Toolbar toolbar;

    private FirebaseAuth firebaseAuth;
    private FirebaseStorage firebaseStorage;
    private FirebaseFirestore firestore;
    private FloatingActionButton floatingActionButton;
    private ArrayList<PostModel> posts = new ArrayList<>();
    private RecyclerView postsRecyclerView;
    private PostRecyclerViewAdapter adapter;
    private int postsCount = -1;
    private boolean postDataFetched = false;
    private LinearLayout linearLayoutIndeterminateProgressBar;
    private LinearLayout linearLayoutNoPostsToLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainpage);
        fishRecognitionImageButton = findViewById(R.id.fishRecognitionImageButton);
        googleMapsButton = findViewById(R.id.googleMapsButton);
        logoutImageButton = findViewById(R.id.logoutImageButton);
        floatingActionButton = findViewById(R.id.floatingActionButton);
        // https://developer.android.com/develop/ui/views/components/appbar/setting-up
        // need to find custom defined toolbar in the xml and replace the vanilla toolbar with it
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        postsRecyclerView = findViewById(R.id.postsRecyclerView);
        linearLayoutIndeterminateProgressBar = findViewById(R.id.linearLayoutIndeterminateProgressBar);
        linearLayoutNoPostsToLoad = findViewById(R.id.linearLayoutNoPostsToLoad);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        linearLayoutIndeterminateProgressBar.setVisibility(View.VISIBLE);

        // get posts by combining sources from firestore and firebase cloud storage
        Thread getPostData = new Thread() {
            @Override
            public void run() {
                super.run();
                getPosts();
            }
        };
        getPostData.start();

        floatingActionButton.setOnClickListener(view -> {
            if(postDataFetched) {
                Intent createPost = new Intent(getApplicationContext(), CreatePost.class);
                startActivity(createPost);
            }
        });

        logoutImageButton.setOnClickListener(view -> {
            if(postDataFetched) {
                firebaseAuth.signOut();
                Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(loginActivity);
            }
        });

        fishRecognitionImageButton.setOnClickListener(view -> {
            if(postDataFetched) {
                Intent fishRecognitionActivity = new Intent(getApplicationContext(), FishRecognitionActivity.class);
                startActivity(fishRecognitionActivity);
            }
        });

        googleMapsButton.setOnClickListener(view -> {
            if(postDataFetched) {
                Intent mapActivity = new Intent(getApplicationContext(), MapActivity.class);
                startActivity(mapActivity);
            }
        });

        Thread getPostCount = new Thread() {
            @Override
            public void run() {
                super.run();
                // get the number of posts so that the thread waitForPostData knows how much posts are in firestore
                firestore.collection("posts").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            postsCount = task.getResult().size();
                        }
                    }
                });
            }
        };
        getPostCount.start();

    Thread waitForPostData = new Thread() {
        @Override
        public void run() {
            super.run();
            while(postDataFetched != true) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                       if (postsCount == 0) {
                           // TODO: add no posts view
                           linearLayoutNoPostsToLoad.setVisibility(View.VISIBLE);
                           linearLayoutIndeterminateProgressBar.setVisibility(View.GONE);
                           setButtonsInteractiveBackgrounds();
                           postDataFetched = true;
                       }
                       if (postsCount == posts.size()) {
                           // sort the posts based on timestamp, firestore is meant to sort them but it is not consistentr
                           // maybe they are fetched in the correct order but the posts are added to the array at different
                           // in a different order since the wait time for some posts in callbacks is shorter than for others
                           // for example, no media for one post and 10 mb video for the other post
                           Collections.sort(posts, new TimestampComparator());
                            //create the adapter with post date and attach it to the recycler view and pass this class as the listener for on click methods in the for the recycle view
                           adapter = new PostRecyclerViewAdapter(getApplicationContext(), posts, MainPageActivity.this);
                           postsRecyclerView.setAdapter(adapter);
                           // wait until the layout has been fully inflated and only then set the flag for data fetches
                           // avoids layout being displayed with some parts no loaded (videos, etc)
                           postsRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()) {
                               @Override
                               public void onLayoutCompleted(RecyclerView.State state) {
                                   super.onLayoutCompleted(state);
                                   linearLayoutIndeterminateProgressBar.setVisibility(View.GONE);
                                   setButtonsInteractiveBackgrounds();
                                   postDataFetched = true;
                               }
                           });
                       }
                    }
                });
            }
        }
    };
    waitForPostData.start();
    }




    public void setButtonsInteractiveBackgrounds() {
        fishRecognitionImageButton.setBackground(getDrawable(R.drawable.layout_background_rounded_corners_toggle_5_gray_opacity_30_to_transparent));
        googleMapsButton.setBackground(getDrawable(R.drawable.layout_background_rounded_corners_toggle_5_gray_opacity_30_to_transparent));
        logoutImageButton.setBackground(getDrawable(R.drawable.layout_background_rounded_corners_toggle_5_gray_opacity_25_to_transparent));
    }



    // minimise the app when the back button is clicked on the main menu
    @Override
    public void onBackPressed() {
        this.moveTaskToBack(true);
    }

    // hide the error messages if were displayed when user decides to go back to this activity
    protected void onResume() {
        super.onResume();
    }


    private void getPosts() {
        // get all posts from firestore ordering based on timestamp
        firestore.collection("posts").orderBy("timestamp", Query.Direction.ASCENDING).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                // get all posts in firestore
                for (QueryDocumentSnapshot post : queryDocumentSnapshots) {
                    String nickname = (String) post.get("nickname");
                    String title = (String) post.get("title");
                    String body = (String) post.get("body");
                    // extract a readable date from the firebase timestamp
                    Date firestoreTimestamp = ((Timestamp) post.get("timestamp")).toDate();
                    DateFormat dateFormatter = new SimpleDateFormat("YYYY/MM/dd HH:MM");
                    String postUploadDate = dateFormatter.format(firestoreTimestamp);
                    String likes = post.get("likes").toString();
                    String userId = post.get("userId").toString();
                    String mimeType = post.get("mimeType").toString();
                    String uniquePostRef = post.getId();
                    ArrayList<String> postLikedBy = (ArrayList<String>) post.get("likedBy");
                    // get media from firestore cloud if exists then create an PostModel object  with media otherwise set media to null
                    StorageReference storageRefMedia = firebaseStorage.getReference().child("/postMedia/" + post.getId());
                    storageRefMedia.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // the post had media associated with it and profile picture is fetched below
                            Uri uriMedia = uri;
                            // add on the media, profile picture and metadata along with standard post components
                            StorageReference storageRefProfPic = firebaseStorage.getReference().child("/profilePictures/" + userId + "/");
                            storageRefProfPic.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] photoBytes) {
                                    byte[] userProfPicture = photoBytes;
                                    PostModel post = new PostModel(getApplicationContext(), title, body, userProfPicture, nickname, postUploadDate, likes, uriMedia, mimeType, uniquePostRef, userId, postLikedBy);
                                    posts.add(post);
                                }
                            });

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // the post did not have any media associated with it and profile picture is fetched below
                            StorageReference storageRefProfPic = firebaseStorage.getReference().child("/profilePictures/" + userId + "/");
                            storageRefProfPic.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] photoBytes) {
                                    byte[] userProfPicture = photoBytes;
                                    PostModel post = new PostModel(getApplicationContext(), title, body, userProfPicture, nickname, postUploadDate, likes, null, null, uniquePostRef, userId, postLikedBy);
                                    posts.add(post);
                                }
                            });
                        }
                    });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                 Log.e(TAG, e.getMessage());
            }
        });
    }


    // handle on-clicks for videos
    @Override
    public void onClickVideoThumbnail(int position) {
        // switch to the play video activity and supply it with the uri of the video that should be displayed
        Intent videoActivity = new Intent(getApplicationContext(), PlayVideoActivity.class);
        String videoUri = posts.get(position).getMedia().toString();
        videoActivity.putExtra("video", videoUri);
        startActivity(videoActivity);
    }

    // handle on-clicks for like buttons

    @Override
    public void onClickLikeButton(int position) {
        // local update
        ArrayList<String> listOfUsers = posts.get(position).getPostLikedBy();
        String currentUser = firebaseAuth.getCurrentUser().getUid();
        if(listOfUsers == null) {
            listOfUsers = new ArrayList<>(Arrays.asList(currentUser));
        } else if (listOfUsers.contains(currentUser)) {
            listOfUsers.remove(currentUser);
        } else if (!listOfUsers.contains(currentUser)) {
            listOfUsers.add(currentUser);
        }
        posts.get(position).setPostLikedBy(listOfUsers);
        posts.get(position).setNumLikes(listOfUsers.size());
        adapter.notifyDataSetChanged();

        // update firestore

        // list of users
        String uniquePostRef = posts.get(position).getUniquePostRef();
        Map<String, Object> listOfUsersMap = new HashMap<>();
        listOfUsersMap.put("likedBy", listOfUsers);
        firestore.collection("posts").document(uniquePostRef).update(listOfUsersMap);

        // num likes
        Map<String, Object> numLikesMap = new HashMap<>();
        numLikesMap.put("likes", listOfUsers.size());
        firestore.collection("posts").document(uniquePostRef).update(numLikesMap);
    }
}
