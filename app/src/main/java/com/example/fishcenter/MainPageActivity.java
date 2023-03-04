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
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MainPageActivity extends AppCompatActivity {

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
    private boolean threadFlag = false;
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
        // get posts by combining sources from firestore and firebase cloud storage
        getPosts();

        floatingActionButton.setOnClickListener(view -> {
            Intent createPost = new Intent(getApplicationContext(), CreatePost.class);
            startActivity(createPost);
        });

        logoutImageButton.setOnClickListener(view -> {
            firebaseAuth.signOut();
            Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(loginActivity);
        });

        fishRecognitionImageButton.setOnClickListener(view -> {
            Intent fishRecognitionActivity = new Intent(getApplicationContext(), FishRecognitionActivity.class);
            startActivity(fishRecognitionActivity);
        });

        googleMapsButton.setOnClickListener(view -> {
            Intent mapActivity = new Intent(getApplicationContext(), MapActivity.class);
            startActivity(mapActivity);
        });


        firestore.collection("posts").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {
                    postsCount = task.getResult().size();
                }
            }
        });

    Thread waitForPostData = new Thread() {
        @Override
        public void run() {
            super.run();
            while(threadFlag != true) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                       if (postsCount == 0) {
                           // TODO: add no posts view
                           linearLayoutNoPostsToLoad.setVisibility(View.VISIBLE);
                           threadFlag = true;
                       }
                       if (postsCount == posts.size()) {
                           linearLayoutNoPostsToLoad.setVisibility(View.GONE);
                           // create the adapter with post date and attach it to the recycler view
                           adapter = new PostRecyclerViewAdapter(getApplicationContext(), posts);
                           postsRecyclerView.setAdapter(adapter);
                           postsRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                           threadFlag = true;
                       }
                    }
                });
                if(threadFlag) {
                    linearLayoutIndeterminateProgressBar.setVisibility(View.GONE);
                }
            }
        }
    };
    waitForPostData.start();
    linearLayoutIndeterminateProgressBar.setVisibility(View.VISIBLE);
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
        firestore.collection("posts").orderBy("timestamp").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                   // get all posts in firestore
                    for (QueryDocumentSnapshot post : task.getResult()) {
                        String nickname = (String) post.get("nickname");
                        String title = (String) post.get("title");
                        String body = (String) post.get("body");
                        // extract a readable date from the firebase timestamp
                        Date firestoreTimestamp = ((Timestamp) post.get("timestamp")).toDate();
                        DateFormat dateFormatter = new SimpleDateFormat("YYYY/MM/dd HH:MM");
                        String postUploadDate = dateFormatter.format(firestoreTimestamp);
                        String likes = post.get("likes").toString();
                        String userId = post.get("userId").toString();
                        // get media from firestore cloud if exists then create an PostModel object  with media otherwise set media to null
                        StorageReference storageRefMedia = firebaseStorage.getReference().child("/postMedia/" + post.getId());
                        storageRefMedia.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uriMedia) {
                                // the post had media associated with it and profile picture is fetched below
                                StorageReference storageRedProfPic = firebaseStorage.getReference().child("/profilePictures/" + userId + "/");
                                storageRedProfPic.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                    @Override
                                    public void onSuccess(byte[] photoBytes) {
                                        PostModel post = new PostModel(getApplicationContext(), title, body, photoBytes, nickname, postUploadDate, likes, uriMedia);
                                        posts.add(post);
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // the post did not have any media associated with it and profile picture is fetched below
                                StorageReference storageRedProfPic = firebaseStorage.getReference().child("/profilePictures/" + userId + "/");
                                storageRedProfPic.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                    @Override
                                    public void onSuccess(byte[] photoBytes) {
                                        PostModel post = new PostModel(getApplicationContext(), title, body, photoBytes, nickname, postUploadDate, likes, null);
                                        posts.add(post);
                                    }
                                });
                            }
                        });
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, e.getMessage());

            }
        });
    }



    /*
    private void getPosts() {
        firestore.collection("users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {
                    // get the top document which is has the user name and a sub-collection posts
                    for (QueryDocumentSnapshot user : task.getResult()) {
                        CollectionReference posts = user.getReference().collection("posts");
                        String nickname = (String) user.get("nickname");
                        // get all posts for this user
                        posts.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                for (QueryDocumentSnapshot posts : task.getResult()) {
                                    String title = (String) posts.get("title");
                                    String body = (String) posts.get("body");
                                    Timestamp timestamp = (Timestamp) posts.get("timestamp");
                                    String likes = posts.get("likes").toString();
                                    String userId = posts.get("userId").toString();

                                    // need to get firebase storage too check if user posted media with this post by matching post id's for this user
                                    StorageReference storageRefMedia = firebaseStorage.getReference().child("/postMedia/" + userId + "/" + posts.getId());
                                    // check if the user has posted any media with this post
                                    storageRefMedia.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            PostModel temp = new PostModel(getApplicationContext(), title, body, null, nickname, timestamp, likes, uri, userId);
                                            StorageReference storageRedProfPic = firebaseStorage.getReference().child("/profilePictures/" + userId + "/");
                                            storageRedProfPic.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri) {
                                                    temp.setProfilePhoto(uri);
                                                    userPosts.add(temp);
                                                }
                                            });
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            PostModel temp = new PostModel(getApplicationContext(), title, body, null, nickname, timestamp, likes, null, userId);
                                            StorageReference storageRedProfPic = firebaseStorage.getReference().child("/profilePictures/" + userId + "/");
                                            storageRedProfPic.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri) {
                                                    temp.setProfilePhoto(uri);
                                                    userPosts.add(temp);
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
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, e.getMessage());
            }
        });

    } */
}
