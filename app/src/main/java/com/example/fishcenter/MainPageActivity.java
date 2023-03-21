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
import androidx.room.Room;

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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    private PostsDatabase postsDatabase;
    private PostsDao postsDao;
    private int postsToLoad = -1;
    private int postsLoaded;
    private boolean syncOver = true;
    private ArrayList<String> postsLikedByUser;
    private ArrayList<String> postsDislikedByUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainpage);
        // https://developer.android.com/develop/ui/views/components/appbar/setting-up
        // need to find custom defined toolbar in the xml and replace the vanilla toolbar with it
        postsRecyclerView = findViewById(R.id.postsRecyclerView);
        linearLayoutNoPostsToLoad = findViewById(R.id.linearLayoutNoPostsToLoad);
        progressSpinnerLayout = findViewById(R.id.progressSpinnerLayout);

        // setup the top application bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // setup the recycler view
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        adapter = new PostRecyclerViewAdapter(getApplicationContext(), posts, MainPageActivity.this);
        postsRecyclerView.setAdapter(adapter);

        // initialise the posts database and the posts dao
        postsDatabase = Room.databaseBuilder(getApplicationContext(), PostsDatabase.class, "postsDatabase").allowMainThreadQueries().build();
        postsDao = postsDatabase.postsDao();

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
                if(syncOver) {
                    syncOver = false;
                    waitForSyncComplete();
                    syncRoomWithFirestoreAndCloudStorage();
                }
            }
        });

        loadPostsFromRoomDatabase();
        // get posts, nickname and user profile picture
        waitForBasicUserDataFromFirestore();
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
            // add the local post to the room database
            LocalPost localPost = (LocalPost) data.getSerializableExtra("localPost");
            postsDao.addLocalPost(localPost);
            loadPostsFromRoomDatabase();
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
        String uniquePostRef = posts.get(position).getUniquePostRef();
        int[] totalLikesArray = updateUserLikesLists(position, uniquePostRef, resCalled);
        // local recycler view update
        posts.get(position).setNumLikes(totalLikesArray[0]);
        posts.get(position).setNumDislikes(totalLikesArray[1]);
        // firebase update user lists
        updateUserPostsLikedListFirestore(uniquePostRef, postsLikedByUser);
        updateUserPostsDislikedListFirestore(uniquePostRef, postsDislikedByUser);
        // update room database
        updateLocalPostInRoomDatabase(uniquePostRef, totalLikesArray[0], totalLikesArray[1]);
        adapter.notifyDataSetChanged();
    }

    private int[] updateUserLikesLists(int position, String uniquePostRef, int resCalled) {
        int numLikes = Integer.parseInt(posts.get(position).getNumLikes());
        int numDislikes = Integer.parseInt(posts.get(position).getNumDislikes());
        int like = 0;
        int dislike = 0;

        if(resCalled == R.id.likesButton) {
           if (postsLikedByUser.contains(uniquePostRef)) {
               postsLikedByUser.remove(uniquePostRef);
               like--;
           } else {
               postsLikedByUser.add(uniquePostRef);
               like++;
           }
            updatePostNumLikesFirestore(uniquePostRef, like);
           if (postsDislikedByUser.contains(uniquePostRef)) {
               postsDislikedByUser.remove(uniquePostRef);
               dislike--;
               updatePostNumDislikesFirestore(uniquePostRef, dislike);
           }
        } else if (resCalled == R.id.dislikesButton) {
           if (postsDislikedByUser.contains(uniquePostRef)) {
               postsDislikedByUser.remove(uniquePostRef);
               dislike--;
           } else {
               postsDislikedByUser.add(uniquePostRef);
               dislike++;
           }
            updatePostNumDislikesFirestore(uniquePostRef, dislike);
           if (postsLikedByUser.contains(uniquePostRef)) {
               postsLikedByUser.remove(uniquePostRef);
               like--;
               updatePostNumLikesFirestore(uniquePostRef, like);
           }
        }
        // update the totals of likes
        numLikes += like;
        numDislikes += dislike;
        return new int[] {numLikes, numDislikes};
    }

    private void updateLocalPostInRoomDatabase(String uniquePostRef, int numLikes, int numDislikes) {
        LocalPost localPostToUpdate = postsDao.findLocalPostByUniqueRef(uniquePostRef);
        localPostToUpdate.setNumLikes(String.valueOf(numLikes));
        localPostToUpdate.setNumDislikes(String.valueOf(numDislikes));
        postsDao.updateLocalPost(localPostToUpdate);
    }

    private void updatePostNumLikesFirestore(String uniquePostRef, int numLikes) {
        firestore.collection("posts").document(uniquePostRef).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Long likesFromFirestore = (Long) documentSnapshot.get("likes");
                likesFromFirestore += numLikes;
                Map<String, Object> numLikesMap = new HashMap<>();
                numLikesMap.put("likes", likesFromFirestore);
                firestore.collection("posts").document(uniquePostRef).update(numLikesMap);
            }
        });

    }

    private void updatePostNumDislikesFirestore(String uniquePostRef, int numDislikes) {
       firestore.collection("posts").document(uniquePostRef).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
           @Override
           public void onSuccess(DocumentSnapshot documentSnapshot) {
               Long dislikesFromFirestore = (Long) documentSnapshot.get("dislikes");
               dislikesFromFirestore += numDislikes;
               Map<String, Object> numDislikesMap = new HashMap<>();
               numDislikesMap.put("dislikes", dislikesFromFirestore);
               firestore.collection("posts").document(uniquePostRef).update(numDislikesMap);
           }
       });
    }

    private void updateUserPostsLikedListFirestore(String uniquePostRef, ArrayList<String> postsLikedByUsers) {
        Map<String, Object> postLikedByUsersMap = new HashMap<>();
        postLikedByUsersMap.put("postsLiked", postsLikedByUsers);
        firestore.collection("users").document(currentUserId).update(postLikedByUsersMap);
    }

    private void updateUserPostsDislikedListFirestore(String uniquePostRef, ArrayList<String> postsDislikedByUsers) {
        Map<String, Object> postDislikedByUsersMap = new HashMap<>();
        postDislikedByUsersMap.put("postsDisliked", postsDislikedByUsers);
        firestore.collection("users").document(currentUserId).update(postDislikedByUsersMap);
    }

    private void waitForBasicUserDataFromFirestore() {
        // fetch both pieces of data and start the spinner
        showSpinnerAndDisableComponents(true);
        getUserNicknameFromFirestore();
        getUserProfPictureFromCloudStorage();
        new Thread() {
            @Override
            public void run() {
                super.run();
                // sleep for 50 seconds if user nickname and userprofile pic have not been fetched
                while (true) {
                    try {
                        Thread.sleep(50);
                        // if both are fetched update the ui with the runOnUiThread by hiding the spinner and brining back interactivity to components
                        if(userNickname != null && userProfilePic != null ) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showSpinnerAndDisableComponents(false);
                                }
                            });
                            break;
                        }
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
                postsLikedByUser = (ArrayList<String>) documentSnapshot.get("postsLiked");
                postsDislikedByUser = (ArrayList<String>) documentSnapshot.get("postsDisliked");
                if(postsLikedByUser == null) {
                    postsLikedByUser = new ArrayList<>();
                }
                if(postsDislikedByUser == null) {
                    postsDislikedByUser = new ArrayList<>();
                }
            }
        });
    }

    private void getUserProfPictureFromCloudStorage() {
        firebaseStorage.getReference("profilePictures/" + currentUserId + "/").getBytes(Integer.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] pictureBytes) {
                userProfilePic = pictureBytes;
            }
        });
    }

    private void loadPostsFromRoomDatabase() {
        new Thread() {
           @Override
           public void run() {
               super.run();
               // get the list of all posts
               List<LocalPost> localPostList = postsDao.getAllLocalPosts();
               for(LocalPost localPost : localPostList) {
                   PostModel recyclerViewPost = new PostModel(getApplicationContext(), localPost.getTitle(), localPost.getBody(), localPost.getProfilePhoto(), localPost.getNickname(), localPost.getPostUploadDate(), localPost.getNumLikes(), localPost.getNumDislikes(), localPost.getMedia(), localPost.getMimeType(), localPost.getUniquePostRef(), localPost.getUserId());
                   // if this post is not in the list add it to the list
                   if (posts.stream().noneMatch(post -> post.getUniquePostRef().equals(recyclerViewPost.getUniquePostRef()))) {
                       posts.add(recyclerViewPost);
                   }
               }
               runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       if(posts.size() == 0) {
                           linearLayoutNoPostsToLoad.setVisibility(View.VISIBLE);
                           postsRecyclerView.setVisibility(View.GONE);
                       } else {
                           postsRecyclerView.setVisibility(View.VISIBLE);
                           linearLayoutNoPostsToLoad.setVisibility(View.GONE);
                           // add this post to posts and re-sort them
                           Collections.sort(posts, new TimestampComparator());
                       }
                       adapter.notifyDataSetChanged();
                       showSpinnerAndDisableComponents(false);
                       postsToLoad = -1;
                       syncOver = true;
                   }
               });
           }
       }.start();
    }

    private void waitForSyncComplete() {
        showSpinnerAndDisableComponents(true);
        new Thread() {
            @Override
            public void run() {
                super.run();
                while(true) {
                    try {
                        Thread.sleep(50);
                        if(postsToLoad == postsLoaded) {
                            loadPostsFromRoomDatabase();
                            break;
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }.start();
    }

    private void syncRoomWithFirestoreAndCloudStorage() {
        firestore.collection("posts").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                postsToLoad = queryDocumentSnapshots.size();
                postsLoaded = 0;
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
                    String numLikes = post.get("likes").toString();
                    String numDislikes = post.get("dislikes").toString();
                    String userId = post.getString("userId");
                    String uniquePostRef = post.getId();
                    // get media from firestore cloud if exists then create an PostModel object  with media otherwise set media to null
                    StorageReference storageRefMedia = firebaseStorage.getReference().child("/postMedia/" + post.getId());
                    storageRefMedia.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // the post had media associated with it and profile picture is fetched below
                            postsLoaded++;
                            String uriMedia = String.valueOf(uri);
                            String mimeType = post.get("mimeType").toString();
                            // add on the media, profile picture and metadata along with standard post components
                            LocalPost localPost = new LocalPost(title, body, userProfilePic, nickname, postUploadDate, numLikes, numDislikes, uriMedia, mimeType, uniquePostRef, userId);
                            if (postsDao.findLocalPostByUniqueRef(uniquePostRef) == null) {
                                postsDao.addLocalPost(localPost);
                            } else {
                                postsDao.updateLocalPost(localPost);
                            }

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            postsLoaded++;
                            // the post did not have any media associated with it and profile picture is fetched below
                            LocalPost localPost = new LocalPost(title, body, userProfilePic, nickname, postUploadDate, numLikes, numDislikes, null, null, uniquePostRef, userId);
                            if (postsDao.findLocalPostByUniqueRef(uniquePostRef) == null) {
                                postsDao.addLocalPost(localPost);
                            } else {
                                postsDao.updateLocalPost(localPost);
                            }
                        }
                    });
                }
            }
        });
    }
}
