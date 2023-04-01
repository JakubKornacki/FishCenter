package com.example.fishcenter;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
    private int numPostsToLoad = -1;
    private int numPostsLoaded;
    private ArrayList<String> postsLikedByUser;
    private ArrayList<String> postsDislikedByUser;
    private boolean userDataLoaded = false;

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
        postsRecyclerView.setItemAnimator(null);

        // initialise the posts database and the posts dao
        postsDatabase = Room.databaseBuilder(getApplicationContext(), PostsDatabase.class, "postsDatabase").fallbackToDestructiveMigration().allowMainThreadQueries().build();
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
                AlertUtilities.createLogoutDialog(MainPageActivity.this, firebaseAuth);
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
                waitForDatabasesSyncComplete();
                syncRoomDatabaseWithFirestoreAndCloudStorage();
            }
        });

        loadPostsFromRoomDatabase();
        // get posts, nickname and user profile picture
        waitForPrimaryUserDataFromFirestore();
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
    public void onClickEitherLikeButton(int position, int buttonCalled) {
        String uniquePostRef = posts.get(position).getUniquePostRef();
        int[] totalLikesArray = updateUserLikesLists(position, uniquePostRef, buttonCalled);
        // local recycler view update
        posts.get(position).setNumLikes(totalLikesArray[0]);
        posts.get(position).setNumDislikes(totalLikesArray[1]);
        // update firestore
        updatePostNumLikesFirestore(uniquePostRef, totalLikesArray[0]);
        updatePostNumDislikesFirestore(uniquePostRef, totalLikesArray[1]);
        // firebase update user lists
        updateUserPostsLikedListFirestore(postsLikedByUser);
        updateUserPostsDislikedListFirestore(postsDislikedByUser);
        // update room database
        updateLocalPostInRoomDatabase(uniquePostRef, totalLikesArray[0], totalLikesArray[1]);
        adapter.notifyItemChanged(position);
    }

    private int[] updateUserLikesLists(int position, String uniquePostRef, int buttonCalled) {
        int numLikes = Integer.parseInt(posts.get(position).getNumLikes());
        int numDislikes = Integer.parseInt(posts.get(position).getNumDislikes());
        if(buttonCalled == R.id.likesButton) {
           if (postsLikedByUser.contains(uniquePostRef)) {
               postsLikedByUser.remove(uniquePostRef);
               numLikes--;
           } else {
               postsLikedByUser.add(uniquePostRef);
               numLikes++;
           }
           if (postsDislikedByUser.contains(uniquePostRef)) {
               postsDislikedByUser.remove(uniquePostRef);
               numDislikes--;
           }
        } else if (buttonCalled == R.id.dislikesButton) {
           if (postsDislikedByUser.contains(uniquePostRef)) {
               postsDislikedByUser.remove(uniquePostRef);
               numDislikes--;
           } else {
               postsDislikedByUser.add(uniquePostRef);
               numDislikes++;
           }
           if (postsLikedByUser.contains(uniquePostRef)) {
               postsLikedByUser.remove(uniquePostRef);
               numLikes--;
           }
        }
        return new int[] {numLikes, numDislikes};
    }

    private void updateLocalPostInRoomDatabase(String uniquePostRef, int numLikes, int numDislikes) {
        LocalPost localPostToUpdate = postsDao.findLocalPostByUniqueRef(uniquePostRef);
        localPostToUpdate.setNumLikes(String.valueOf(numLikes));
        localPostToUpdate.setNumDislikes(String.valueOf(numDislikes));
        postsDao.updateLocalPost(localPostToUpdate);
    }

    private void updatePostNumLikesFirestore(String uniquePostRef, int numLikes) {
        Map<String, Object> numLikesMap = new HashMap<>();
        numLikesMap.put("likes", numLikes);
        firestore.collection("posts").document(uniquePostRef).update(numLikesMap);
    }

    private void updatePostNumDislikesFirestore(String uniquePostRef, int numDislikes) {
        Map<String, Object> numDislikesMap = new HashMap<>();
        numDislikesMap.put("dislikes", numDislikes);
        firestore.collection("posts").document(uniquePostRef).update(numDislikesMap);
    }

    private void updateUserPostsLikedListFirestore(ArrayList<String> postsLikedByUsers) {
        Map<String, Object> postLikedByUsersMap = new HashMap<>();
        postLikedByUsersMap.put("postsLiked", postsLikedByUsers);
        firestore.collection("users").document(currentUserId).update(postLikedByUsersMap);
    }

    private void updateUserPostsDislikedListFirestore(ArrayList<String> postsDislikedByUsers) {
        Map<String, Object> postDislikedByUsersMap = new HashMap<>();
        postDislikedByUsersMap.put("postsDisliked", postsDislikedByUsers);
        firestore.collection("users").document(currentUserId).update(postDislikedByUsersMap);
    }

    private void waitForPrimaryUserDataFromFirestore() {
        // fetch both pieces of data and start the spinner
        showSpinnerAndDisableComponents(true);
        getUserNicknameFromFirestore();
        getUserProfPictureFromCloudStorage();
        new Thread() {
            @Override
            public void run() {
                super.run();
                // sleep for 200 milliseconds if user nickname and userprofile pic have not been fetched
                while (true) {
                    try {
                        Thread.sleep(200);
                        // if both are fetched update the ui with the runOnUiThread by hiding the spinner and brining back interactivity to components
                        if(userDataLoaded && userProfilePic != null ) {
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
                userDataLoaded = true;
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
               // get the list of all posts from room database
               List<LocalPost> localPostList = postsDao.getAllLocalPosts();
               posts.clear();
               for(LocalPost localPost : localPostList) {
                   PostModel recyclerViewPost = new PostModel(MainPageActivity.this, localPost.getTitle(), localPost.getBody(), localPost.getProfilePhoto(), localPost.getNickname(), localPost.getPostUploadDate(), localPost.getNumLikes(), localPost.getNumDislikes(), localPost.getMedia(), localPost.getMimeType(), localPost.getUniquePostRef(), localPost.getUserId());
                   posts.add(recyclerViewPost);
               }
               // sort the posts as they do not come in a sorted order from the external databases
               posts.sort(new TimestampComparator());
               runOnUiThread(new Runnable() {

                   @Override
                   public void run() {
                       if(posts.size() == 0) {
                           linearLayoutNoPostsToLoad.setVisibility(View.VISIBLE);
                           postsRecyclerView.setVisibility(View.GONE);
                       } else {
                           postsRecyclerView.setVisibility(View.VISIBLE);
                           linearLayoutNoPostsToLoad.setVisibility(View.GONE);
                           
                       }
                       adapter.notifyDataSetChanged();
                       showSpinnerAndDisableComponents(false);
                       numPostsToLoad = -1;
                   }
               });
           }
       }.start();
    }

    private void waitForDatabasesSyncComplete() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                while(true) {
                    try {
                        Thread.sleep(200);
                        if(numPostsToLoad == numPostsLoaded) {
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

    private boolean checkIfSynchronisationIsNecessary(int numPosts) {
        if(numPosts == posts.size() && numPosts != 0) {
            Toast.makeText(MainPageActivity.this, "Your posts list is up to date!", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            showSpinnerAndDisableComponents(true);
            return true;
        }
    }

    private void syncRoomDatabaseWithFirestoreAndCloudStorage() {
        firestore.collection("posts").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                numPostsToLoad = queryDocumentSnapshots.size();
                numPostsLoaded = 0;
                if(!checkIfSynchronisationIsNecessary(numPostsToLoad)) {
                    return;
                }
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
                    // the following code downloads the uri of the media rather than the files itself this
                    // is done in order to note flood the user memory with posts, rather than that each
                    // local post has the download url of the media which it can downloaded at runtime
                    // and when the application closes the downloaded data is not stored the user device
                    storageRefMedia.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            LocalPost newLocalPost;
                            // the post has media
                            if(task.isSuccessful()) {
                                Uri uri = task.getResult();
                                String uriMedia = String.valueOf(uri);
                                String mimeType = post.get("mimeType").toString();
                                newLocalPost = new LocalPost(title, body, userProfilePic, nickname, postUploadDate, numLikes, numDislikes, uriMedia, mimeType, uniquePostRef, userId);
                            } else {
                                // this post does not contain any media
                                newLocalPost = new LocalPost(title, body, userProfilePic, nickname, postUploadDate, numLikes, numDislikes, null,null, uniquePostRef, userId);
                            }
                            // update the Room database
                            if (postsDao.findLocalPostByUniqueRef(uniquePostRef) == null) {
                                postsDao.addLocalPost(newLocalPost);
                            } else {
                                postsDao.updateLocalPost(newLocalPost);
                            }
                            numPostsLoaded++;
                        }
                    });
                }
            }
        });
    }
}

