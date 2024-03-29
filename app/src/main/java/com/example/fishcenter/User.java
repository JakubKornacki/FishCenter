package com.example.fishcenter;

import java.io.Serializable;
import java.util.ArrayList;

public class User implements Serializable {

    private final String nickname;
    private final String userUid;
    private final ArrayList<String> postsLiked;
    private final ArrayList<String> postsDisliked;
    private final byte[] profilePicture;

    public User (String nickname, String userUid, ArrayList<String> postsLiked, ArrayList<String> postsDisliked, byte[] profilePicture) {
        this.nickname = nickname;
        this.userUid = userUid;
        if(postsLiked == null) {
            this.postsLiked = new ArrayList<>();
        } else {
            this.postsLiked = postsLiked;
        }
        if(postsDisliked == null) {
            this.postsDisliked = new ArrayList<>();
        } else {
            this.postsDisliked = postsDisliked;
        }
        this.profilePicture = profilePicture;
    }

    public String getNickname() {
        return nickname;
    }

    public ArrayList<String> getPostsLiked() {
        return postsLiked;
    }

    public ArrayList<String> getPostsDisliked() {
        return postsDisliked;
    }

    public byte[] getProfilePicture() {
        return profilePicture;
    }

    public String getUserUid() {
        return userUid;
    }
}
