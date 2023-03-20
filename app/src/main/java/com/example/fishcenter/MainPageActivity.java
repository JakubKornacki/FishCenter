package com.example.fishcenter;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class MainPageActivity extends AppCompatActivity implements OnClickListener {
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private ImageButton fishRecognitionButton;
    private ImageButton googleMapsButton;
    private ImageButton logoutImageButton;
    private ImageButton reloadPostsButton;
    private ArrayList<PostModel> posts = new ArrayList<>();
    private RecyclerView postsRecyclerView;
    private PostRecyclerViewAdapter adapter;
    private FloatingActionButton createPostButton;

    private LinearLayout progressSpinnerLayout;
    private LinearLayout linearLayoutNoPostsToLoad;
    private String currentUserId = FirebaseAuth.getInstance().getUid();
    private byte[] userProfilePic = null;
    private String userNickname = null;
    private int postsArrayListSize = -1;
    private boolean postDataFetched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainpage);
        // https://developer.android.com/develop/ui/views/components/appbar/setting-up
        // need to find custom defined toolbar in the xml and replace the vanilla toolbar with it
        postsRecyclerView = findViewById(R.id.postsRecyclerView);
        linearLayoutNoPostsToLoad = findViewById(R.id.linearLayoutNoPostsToLoad);
        progressSpinnerLayout = findViewById(R.id.progressSpinnerLayout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        postsRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        adapter = new PostRecyclerViewAdapter(getApplicationContext(), posts, MainPageActivity.this);
        postsRecyclerView.setAdapter(adapter);

        createPostButton = findViewById(R.id.createPostButton);
        createPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent createPost = new Intent(getApplicationContext(), CreatePost.class);
                createPost.putExtra("profilePicture", userProfilePic);
                createPost.putExtra("userNickname", userNickname);
                startActivityForResult(createPost, 1);
            }
        });

        logoutImageButton = findViewById(R.id.logoutImageButton);
        logoutImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createLogoutDialog(firebaseAuth);
            }
        });

        fishRecognitionButton = findViewById(R.id.fishRecognitionImageButton);
        fishRecognitionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent fishRecognitionActivity = new Intent(getApplicationContext(), FishRecognitionActivity.class);
                startActivity(fishRecognitionActivity);
            }
        });

        googleMapsButton = findViewById(R.id.googleMapsButton);
        googleMapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mapActivity = new Intent(getApplicationContext(), MapActivity.class);
                startActivity(mapActivity);
            }
        });

        reloadPostsButton = findViewById(R.id.reloadPostsButton);
        reloadPostsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSpinnerAndDisableComponents(true);
                postDataFetched = false;
                postsArrayListSize = -1;
                posts.clear();
                combinePostsFromFirestoreAndCloudStorage();
                waitForDataFromFirebaseAndCloudStorage();
            }
        });
        // get posts, nickname and user profile picture
        showSpinnerAndDisableComponents(true);
        getUserProfPictureFromCloudStorage();
        getUserNicknameFromFirestore();
        combinePostsFromFirestoreAndCloudStorage();
        waitForDataFromFirebaseAndCloudStorage();
    }

    private void showSpinnerAndDisableComponents(boolean flag) {
        reloadPostsButton.setClickable(!flag);
        fishRecognitionButton.setClickable(!flag);
        googleMapsButton.setClickable(!flag);
        createPostButton.setClickable(!flag);
        if(flag) {
            progressSpinnerLayout.setVisibility(View.VISIBLE);
            reloadPostsButton.setBackground(null);
            fishRecognitionButton.setBackground(null);
            googleMapsButton.setBackground(null);
        } else {
            progressSpinnerLayout.setVisibility(View.INVISIBLE);
            reloadPostsButton.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_25_to_transparent));
            fishRecognitionButton.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_30_to_transparent));
            googleMapsButton.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_30_to_transparent));
        }
    }

    public void createLogoutDialog(FirebaseAuth firebaseAuthInstance) {
        AlertDialog.Builder logoutDialog = new AlertDialog.Builder(MainPageActivity.this);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // get data sent back from create post so that the new post can be displayed on the user screen
        if(resultCode == Activity.RESULT_OK && requestCode == 1) {
            // get data passed back from the createPost activity
            LocalPost localPost = (LocalPost) data.getSerializableExtra("localPost");
            PostModel newPost = new PostModel(
                    getApplicationContext(),
                    localPost.getTitle(),
                    localPost.getBody(),
                    userProfilePic,
                    userNickname,
                    localPost.getPostUploadDate(),
                    localPost.getNumLikes(),
                    localPost.getMedia(),
                    localPost.getMimeType(),
                    localPost.getUniquePostRef(),
                    currentUserId,
                    localPost.getPostLikedBy(),
                    localPost.getPostDislikedBy()
            );
            // add this post to posts and re-sort them
            posts.add(newPost);
            Collections.sort(posts, new TimestampComparator());
            // notify the recyclerview adapter of the change in data
            adapter.notifyDataSetChanged();
        }
    }

    // minimise the app when the back button is clicked on the main menu
    @Override
    public void onBackPressed() {
        this.moveTaskToBack(true);
    }

    // handle on-clicks for videos
    @Override
    public void onClickVideoThumbnail(int position) {
        // switch to the play video activity and supply it with the uri of the video that should be displayed
        Intent videoActivity = new Intent(getApplicationContext(), PlayVideoActivity.class);
        String videoUri = posts.get(position).getMedia();
        videoActivity.putExtra("video", videoUri);
        startActivity(videoActivity);
    }

    // handle on-clicks for like buttons
    @Override
    public void onClickEitherLikeButton(int position, int resCalled) {
        ArrayList<ArrayList<String>> lists = updateLocalLikesLists(position, resCalled);
        updateFirestoreLikedByList(position, lists.get(0));
        updateFirestoreDislikedByList(position, lists.get(1));
        int totalLikes = calculateTotalLikes(lists.get(0), lists.get(1));
        updateFirestoreNumLikes(position, totalLikes);
        posts.get(position).setNumLikes(totalLikes);
        adapter.notifyDataSetChanged();
    }

    private int calculateTotalLikes(ArrayList<String> likesList, ArrayList<String> dislikesList) {
        int totalLikes = 0;
        if(likesList != null && dislikesList != null) {
            totalLikes = likesList.size() + (dislikesList.size() * -1);
        } else if (likesList != null) {
            totalLikes = likesList.size();
        } else if(dislikesList != null) {
            totalLikes = dislikesList.size() * -1;
        }
        return totalLikes;
    }

    private void updateFirestoreNumLikes(int position, int numLikes) {
        String uniquePostRef = posts.get(position).getUniquePostRef();
        Map<String, Object> numLikesMap = new HashMap<>();
        numLikesMap.put("likes", numLikes);
        firestore.collection("posts").document(uniquePostRef).update(numLikesMap);
    }


    private ArrayList<ArrayList<String>> updateLocalLikesLists(int position, int resCalled) {
        ArrayList<String> listOfUsersLiking = posts.get(position).getPostLikedBy();
        ArrayList<String> listOfUsersDisliking = posts.get(position).getPostDislikedBy();

        if(resCalled == R.id.likesButton) {
            if (listOfUsersLiking == null) {
                listOfUsersLiking = new ArrayList<>(Arrays.asList(currentUserId));
            } else if (listOfUsersLiking.contains(currentUserId)) {
                listOfUsersLiking.remove(currentUserId);
            } else {
                listOfUsersLiking.add(currentUserId);
            }
            if(listOfUsersDisliking != null) {
                if (listOfUsersDisliking.contains(currentUserId)) {
                    listOfUsersDisliking.remove(currentUserId);
                }
            }
        } else if (resCalled == R.id.dislikesButton) {
            if (listOfUsersDisliking == null) {
                listOfUsersDisliking = new ArrayList<>(Arrays.asList(currentUserId));
            } else if (listOfUsersDisliking.contains(currentUserId)) {
                listOfUsersDisliking.remove(currentUserId);
            } else {
                listOfUsersDisliking.add(currentUserId);
            }
            if(listOfUsersLiking != null) {
                if (listOfUsersLiking.contains(currentUserId)) {
                    listOfUsersLiking.remove(currentUserId);
                }
            }
        }

        // local update
        posts.get(position).setPostLikedBy(listOfUsersLiking);
        posts.get(position).setPostDislikedBy(listOfUsersDisliking);
        // arraylist containing the lists
        ArrayList<ArrayList<String>> lists = new ArrayList<>();
        lists.add(listOfUsersLiking);
        lists.add(listOfUsersDisliking);
        return lists;
    }

    private void updateFirestoreLikedByList(int position, ArrayList<String> listOfUsersLiking) {
        String uniquePostRef = posts.get(position).getUniquePostRef();
        Map<String, Object> listOfUsersLikingMap = new HashMap<>();
        listOfUsersLikingMap.put("likedBy", listOfUsersLiking);
        firestore.collection("posts").document(uniquePostRef).update(listOfUsersLikingMap);
    }

    private void updateFirestoreDislikedByList(int position, ArrayList<String> listOfUsersDisliking) {
        String uniquePostRef = posts.get(position).getUniquePostRef();
        Map<String, Object> listOfUsersDislikingMap = new HashMap<>();
        listOfUsersDislikingMap.put("dislikedBy", listOfUsersDisliking);
        firestore.collection("posts").document(uniquePostRef).update(listOfUsersDislikingMap);
    }

    private void waitForDataFromFirebaseAndCloudStorage() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                // sleep for 50 seconds if posts, user nickname and userprofile pic have not been fetched
                while (!postDataFetched || userNickname == null || userProfilePic == null) {
                    try {
                        Thread.sleep(50);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (postsArrayListSize == 0) {
                                    linearLayoutNoPostsToLoad.setVisibility(View.VISIBLE);
                                    postsRecyclerView.setVisibility(View.GONE);
                                    adapter.notifyDataSetChanged();
                                    showSpinnerAndDisableComponents(false);
                                    postDataFetched = true;
                                } else if (postsArrayListSize == posts.size()) {
                                    postsRecyclerView.setVisibility(View.VISIBLE);
                                    linearLayoutNoPostsToLoad.setVisibility(View.GONE);
                                    // sort the posts internally as posts are not fetched based on timestamp, most likely because of other async calls to cloud storage as well
                                    Collections.sort(posts, new TimestampComparator());
                                    //notify the adapter of data change
                                    adapter.notifyDataSetChanged();
                                    showSpinnerAndDisableComponents(false);
                                    postDataFetched = true;
                                }
                            }
                        });
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }.start();
    }

    private void getUserNicknameFromFirestore() {
        firestore.collection("users").document(currentUserId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                userNickname = documentSnapshot.getString("nickname");
            }
        });
    }

    private void getUserProfPictureFromCloudStorage() {
        firebaseStorage.getReference("profilePictures/" + currentUserId + "/").getBytes(Integer.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                userProfilePic = bytes;
            }
        });
    }

    public void combinePostsFromFirestoreAndCloudStorage() {
        firestore.collection("posts").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                postsArrayListSize = queryDocumentSnapshots.size();
                // get all posts in firestore
                for (QueryDocumentSnapshot post : queryDocumentSnapshots) {
                    String nickname = post.getString("nickname");
                    String title = post.getString("title");
                    String body = post.getString("body");
                    // extract a readable date from the firebase timestamp
                    Date firestoreTimestamp = ((Timestamp) post.get("timestamp")).toDate();
                    DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                    String postUploadDate = dateFormatter.format(firestoreTimestamp);
                    // saved in firestore as number, need to be converted to string
                    String likes = post.get("likes").toString();
                    String userId = post.getString("userId");
                    String uniquePostRef = post.getId();
                    ArrayList<String> postLikedBy = (ArrayList<String>) post.get("likedBy");
                    ArrayList<String> postDislikedBy = (ArrayList<String>) post.get("dislikedBy");
                    // get media from firestore cloud if exists then create an PostModel object  with media otherwise set media to null
                    StorageReference storageRefMedia = firebaseStorage.getReference().child("/postMedia/" + post.getId());
                    storageRefMedia.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // the post had media associated with it and profile picture is fetched below
                            String uriMedia = String.valueOf(uri);
                            String mimeType = post.get("mimeType").toString();
                            // add on the media, profile picture and metadata along with standard post components
                            PostModel post = new PostModel(getApplicationContext(), title, body, userProfilePic, nickname, postUploadDate, likes, uriMedia, mimeType, uniquePostRef, userId, postLikedBy, postDislikedBy);
                            posts.add(post);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // the post did not have any media associated with it and profile picture is fetched below
                            PostModel post = new PostModel(getApplicationContext(), title, body, userProfilePic, nickname, postUploadDate, likes, null, null, uniquePostRef, userId, postLikedBy, postDislikedBy);
                            posts.add(post);
                        }
                    });
                }
            }
        });
    }
}
