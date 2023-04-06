package com.example.fishcenter;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;

public class UserController {

    private final FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private final String currentUserId;
    private User currentUser;
    private final UserCallback userCallback;

    public UserController(String currentUserId, UserCallback userCallback) {
        this.currentUserId = currentUserId;
        this.userCallback = userCallback;
    }

    public void getUserData() {
        firebaseFirestore.collection("users").document(currentUserId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                String userNickname = documentSnapshot.getString("nickname");
                ArrayList<String> postsLikedByUser = (ArrayList<String>) documentSnapshot.get("postsLiked");
                ArrayList<String> postsDislikedByUser = (ArrayList<String>) documentSnapshot.get("postsDisliked");

                firebaseStorage.getReference("profilePictures/" + currentUserId + "/").getBytes(Integer.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] pictureBytes) {
                        currentUser = new User(userNickname, currentUserId, postsLikedByUser, postsDislikedByUser, pictureBytes);
                        userCallback.userDataReady(currentUser);
                    }
                });
            }
        });
    }
}
