package com.example.fishcenter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
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
import java.util.Locale;
import java.util.Map;

public class PostsController {
    private final FirebaseFirestore firestore =  FirebaseFirestore.getInstance();
    private final FirebaseStorage firebaseStorage =  FirebaseStorage.getInstance();

    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private final String currentUserId;
    private final PostsCallback postsCallback;

    private final PostsDatabase postsDatabase;
    private final PostsDao postsDao;
    private final Context context;

    public PostsController(Context context, PostsCallback postsCallback) {
        this.postsCallback = postsCallback;
        currentUserId = firebaseAuth.getCurrentUser().getUid();
        this.context = context;
        // initialise the posts database and the posts dao
        postsDatabase = Room.databaseBuilder(context, PostsDatabase.class, "postsDatabase").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        postsDao = postsDatabase.postsDao();
    }


    public void loadPostsFromRoomDatabase() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                // get the list of all posts from room database
                List<LocalPost> localPostList = postsDao.getAllLocalPosts();
                ArrayList<PostModel> recyclerViewPosts = convertLocalPostsToRecyclerViewPosts(localPostList);
                postsCallback.userPostsReady(recyclerViewPosts);
            }
        }.start();
    }

    private void updatePostInRoomDatabase(ArrayList<LocalPost> localPosts) {
       for(LocalPost localPost : localPosts) {
           String uniquePostRef = localPost.getUniquePostRef();
           // update the Room database
           if (postsDao.findLocalPostByUniqueRef(uniquePostRef) == null) {
               postsDao.addLocalPost(localPost);
           } else {
               postsDao.updateLocalPost(localPost);
           }
       }
       // load new posts after sync complete
       loadPostsFromRoomDatabase();
    }

    public void addLocalPostToRoomDatabase(LocalPost localPost) {
        postsDao.addLocalPost(localPost);
    }


    private ArrayList<PostModel> convertLocalPostsToRecyclerViewPosts(List<LocalPost> localPosts) {
        ArrayList<PostModel> recyclerViewPosts = new ArrayList<>();
        for(LocalPost localPost : localPosts){
            recyclerViewPosts.add(new PostModel(context, localPost));
        }
        return recyclerViewPosts;
    }

    public void synchronizeRoomDatabaseWithFirestore(int numPostsExisting) {
        firestore.collection("posts").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                ArrayList<LocalPost> localPosts = new ArrayList<>();
                int numPostsToLoad = queryDocumentSnapshots.size();
                if(numPostsToLoad == numPostsExisting) {
                    postsCallback.isSynchronisationNecessary(false);
                    return;
                } else {
                    postsCallback.isSynchronisationNecessary(true);
                }
                // get all localPosts in firestore
                for (QueryDocumentSnapshot post : queryDocumentSnapshots) {
                    String nickname = post.getString("nickname");
                    String title = post.getString("title");
                    String body = post.getString("body");
                    // extract a readable date from the firebase timestamp
                    Date firestoreTimestamp = ((Timestamp) post.get("timestamp")).toDate();
                    DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.UK);
                    String postUploadDate = dateFormatter.format(firestoreTimestamp);
                    // saved in firestore as number, need to be converted to string
                    String numLikes = post.get("likes").toString();
                    String numDislikes = post.get("dislikes").toString();
                    String userId = post.getString("userId");
                    String uniquePostRef = post.getId();
                    StorageReference profilePictureReference = firebaseStorage.getReference().child("/profilePictures/" + userId);
                    // get media from firestore cloud if exists then create an PostModel object  with media otherwise set media to null
                    StorageReference postMediaReference = firebaseStorage.getReference().child("/postMedia/" + post.getId());
                    profilePictureReference.getBytes(Integer.MAX_VALUE).addOnCompleteListener(new OnCompleteListener<byte[]>() {
                        @Override
                        public void onComplete(@NonNull Task<byte[]> task) {
                            byte[] userProfilePictureBytes = task.getResult();
                            // the following code downloads the uri of the media rather than the files itself this
                            // is done in order to note flood the user memory with localPosts, rather than that each
                            // local post has the download url of the media which it can downloaded at runtime
                            // and when the application closes the downloaded data is not stored the user device
                            postMediaReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    LocalPost newLocalPost;
                                    // the post has media
                                    if(task.isSuccessful()) {
                                        Uri uri = task.getResult();
                                        String uriMedia = String.valueOf(uri);
                                        String mimeType = post.getString("mimeType");
                                        newLocalPost = new LocalPost(title, body, userProfilePictureBytes, nickname, postUploadDate, numLikes, numDislikes, uriMedia, mimeType, uniquePostRef, userId);
                                    } else {
                                        // this post does not contain any media
                                        newLocalPost = new LocalPost(title, body, userProfilePictureBytes, nickname, postUploadDate, numLikes, numDislikes, null,null, uniquePostRef, userId);
                                    }
                                    // update room database
                                    // add the new post which will be displayed in the recycler view
                                    localPosts.add(newLocalPost);
                                    // if this is the last post return sorted arraylist to the main page
                                    if(localPosts.size() == numPostsToLoad) {
                                        updatePostInRoomDatabase(localPosts);
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });
    }


    public void savePostInBackend(String postBody, String postTitle, User user, boolean mediaSelected, String mimeType, Uri userMediaUri) {
        // mark the timestamp at the beginning of creating a post
        String currentUserId = user.getUserUid();
        String userNickname = user.getNickname();
        byte[] userProfilePicture = user.getProfilePicture();
        Map<String, Object> post = new HashMap<>();
        Timestamp timestamp = Timestamp.now();
        DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH);
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
                // create a local copy of the post
                LocalPost localPost = new LocalPost(postTitle, postBody, userProfilePicture, userNickname, postUploadDate, "0", "0", String.valueOf(userMediaUri), mimeType, uniquePostRef, currentUserId);
                postsCallback.newPostSaved(localPost);
            }
        });
    }

    public void handleUserLikesAndDislikes(PostModel post, User user, int position, int buttonCalled){
        // if user has not been loaded yet return
        if(user == null) {
            return;
        }
        String uniquePostRef = post.getUniquePostRef();
        ArrayList<String> postsLikedByUser = user.getPostsLiked();
        ArrayList<String> postsDislikedByUser = user.getPostsDisliked();
        int[] totalLikesArray = updateUserLikesLists(post, uniquePostRef, postsLikedByUser, postsDislikedByUser, buttonCalled);
        // local recycler view update
        post.setNumLikes(totalLikesArray[0]);
        post.setNumDislikes(totalLikesArray[1]);
        // update firestore
        updatePostNumLikesFirestore(uniquePostRef, totalLikesArray[0]);
        updatePostNumDislikesFirestore(uniquePostRef, totalLikesArray[1]);
        // firebase update user lists
        updateUserPostsLikedListFirestore(postsLikedByUser);
        updateUserPostsDislikedListFirestore(postsDislikedByUser);
        // update room database
        updateLocalPostInRoomDatabase(uniquePostRef, totalLikesArray[0], totalLikesArray[1]);
        postsCallback.userPostUpdated(position);
    }

    public int[] updateUserLikesLists(PostModel post, String uniquePostRef, ArrayList<String> postsLikedByUser, ArrayList<String> postsDislikedByUser, int buttonCalled) {
        int numLikes = Integer.parseInt(post.getNumLikes());
        int numDislikes = Integer.parseInt(post.getNumDislikes());
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


    public void updateLocalPostInRoomDatabase(String uniquePostRef, int numLikes, int numDislikes) {
        LocalPost localPostToUpdate = postsDao.findLocalPostByUniqueRef(uniquePostRef);
        localPostToUpdate.setNumLikes(String.valueOf(numLikes));
        localPostToUpdate.setNumDislikes(String.valueOf(numDislikes));
        postsDao.updateLocalPost(localPostToUpdate);
    }


    public void updatePostNumLikesFirestore(String uniquePostRef, int numLikes) {
        Map<String, Object> numLikesMap = new HashMap<>();
        numLikesMap.put("likes", numLikes);
        firestore.collection("posts").document(uniquePostRef).update(numLikesMap);
    }

    public void updatePostNumDislikesFirestore(String uniquePostRef, int numDislikes) {
        Map<String, Object> numDislikesMap = new HashMap<>();
        numDislikesMap.put("dislikes", numDislikes);
        firestore.collection("posts").document(uniquePostRef).update(numDislikesMap);
    }

    public void updateUserPostsLikedListFirestore(ArrayList<String> postsLikedByUsers) {
        Map<String, Object> postLikedByUsersMap = new HashMap<>();
        postLikedByUsersMap.put("postsLiked", postsLikedByUsers);
        firestore.collection("users").document(currentUserId).update(postLikedByUsersMap);
    }

    public void updateUserPostsDislikedListFirestore(ArrayList<String> postsDislikedByUsers) {
        Map<String, Object> postDislikedByUsersMap = new HashMap<>();
        postDislikedByUsersMap.put("postsDisliked", postsDislikedByUsers);
        firestore.collection("users").document(currentUserId).update(postDislikedByUsersMap);
    }

}
