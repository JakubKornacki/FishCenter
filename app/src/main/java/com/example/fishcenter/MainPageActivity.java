package com.example.fishcenter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;


public class MainPageActivity extends AppCompatActivity implements OnClickListener, PostsCallback, UserCallback {
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private ImageButton fishRecognitionButton;
    private ImageButton googleMapsButton;
    private ImageButton reloadPostsButton;
    private final ArrayList<PostModel> posts = new ArrayList<>();
    private RecyclerView postsRecyclerView;
    private PostRecyclerViewAdapter adapter;
    private FloatingActionButton createPostButton;

    private LinearLayout progressSpinnerLayout;
    private LinearLayout linearLayoutNoPostsToLoad;
    private User currentUser;
    private PostsController postsController;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainpage);
        // https://developer.android.com/develop/ui/views/components/appbar/setting-up
        // need to find custom defined toolbar in the xml and replace the vanilla toolbar with it
        postsRecyclerView = findViewById(R.id.postsRecyclerView);
        linearLayoutNoPostsToLoad = findViewById(R.id.linearLayoutNoPostsToLoad);
        progressSpinnerLayout = findViewById(R.id.progressSpinnerLayout);

        context = getApplicationContext();

        postsController = new PostsController(MainPageActivity.this, this);
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        UserController userController = new UserController(currentUserId, this);
        // setup the top application bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        // setup the recycler view
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        adapter = new PostRecyclerViewAdapter(getApplicationContext(), posts, MainPageActivity.this);
        postsRecyclerView.setAdapter(adapter);
        postsRecyclerView.setItemAnimator(null);


        createPostButton = findViewById(R.id.createPostButton);
        createPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent createPost = new Intent(getApplicationContext(), CreatePost.class);
                createPost.putExtra("currentUser", currentUser);
                startActivityForResult(createPost, 1);
            }
        });

        ImageButton logoutImageButton = findViewById(R.id.logoutImageButton);
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
                mapActivity.putExtra("currentUser", currentUser);
                startActivity(mapActivity);
            }
        });

        reloadPostsButton = findViewById(R.id.reloadPostsButton);
        reloadPostsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int numPostsExisting = posts.size();
                postsController.synchronizeRoomDatabaseWithFirestore(numPostsExisting);
            }
        });

        showSpinnerAndDisableComponents(true);
        userController.getUserData();
        postsController.loadPostsFromRoomDatabase();
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
            reloadPostsButton.setBackground(AppCompatResources.getDrawable(context, R.drawable.background_rounded_corners_toggle_5_gray_opacity_25_to_transparent));
            fishRecognitionButton.setBackground(AppCompatResources.getDrawable(context, R.drawable.background_rounded_corners_toggle_5_gray_opacity_30_to_transparent));
            googleMapsButton.setBackground(AppCompatResources.getDrawable(context, R.drawable.background_rounded_corners_toggle_5_gray_opacity_30_to_transparent));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // get data sent back from create post so that the new post can be displayed on the user screen
        if(resultCode == Activity.RESULT_OK && requestCode == 1) {
            // add the local post to the room database
            LocalPost localPost = (LocalPost) data.getSerializableExtra("localPost");
            postsController.addLocalPostToRoomDatabase(localPost);
            postsController.loadPostsFromRoomDatabase();
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
        PostModel post = posts.get(position);
        postsController.handleUserLikesAndDislikes(post, currentUser, position, buttonCalled);
    }

    @Override
    public void userDataReady(User currentUser) {
        this.currentUser = currentUser;
        showSpinnerAndDisableComponents(false);
    }


    private void addNewPostsToPostsList(ArrayList<PostModel> userPosts) {
        for(int i = 0; i < userPosts.size(); i++) {
            boolean shouldAddPost = isPostNotInTheList(userPosts.get(i));
            if(shouldAddPost) {
                posts.add(userPosts.get(i));
            }
        }
        posts.sort(new TimestampComparator());
    }

    private boolean isPostNotInTheList(PostModel post) {
        for(int i = 0; i < posts.size(); i++) {
            if(posts.get(i).getUniquePostRef().equals(post.getUniquePostRef())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void userPostsReady(ArrayList<PostModel> userPosts) {
        addNewPostsToPostsList(userPosts);
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
                // only hide the spinner if the user data has been downloaded
                if(currentUser != null) {
                    showSpinnerAndDisableComponents(false);
                }
                adapter.notifyItemRangeInserted(0,posts.size());
            }
        });
    }

    @Override
    public void userPostUpdated(int position) {
        adapter.notifyItemChanged(position);
    }

    @Override
    public void newPostSaved(LocalPost newPost) {
        // nothing to do here
    }

    @Override
    public void isSynchronisationNecessary(boolean isNecessary) {
        if(isNecessary) {
            showSpinnerAndDisableComponents(true);
        } else {
            Toast.makeText(MainPageActivity.this, "Your posts are up to date!", Toast.LENGTH_SHORT).show();
        }
    }
}

