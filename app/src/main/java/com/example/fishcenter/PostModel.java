package com.example.fishcenter;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;

public class PostModel extends RecyclerView implements Serializable {
    private String title;
    private String body;
    private byte[] profilePhoto;
    private String nickname;
    private String postUploadDate;
    private String numLikes;
    private String media;
    private String mimeType;
    private String uniquePostRef;
    private String userId;
    private ArrayList<String> postLikedBy;
    private ArrayList<String> postDislikedBy;

    public PostModel(@NonNull Context context) {
        super(context);
    }

    public PostModel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PostModel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PostModel(@NonNull Context context, String title, String body, byte[] profilePhoto, String nickname, String postUploadDate, String numLikes, String media, String mimeType, String uniquePostRef, String userId, ArrayList<String> postLikedBy) {
        super(context);
        this.title = title;
        this.body = body;
        this.profilePhoto = profilePhoto;
        this.nickname = nickname;
        this.postUploadDate = postUploadDate;
        this.numLikes = numLikes;
        this.media = media;
        this.mimeType = mimeType;
        this.uniquePostRef = uniquePostRef;
        this.userId = userId;
    }

    public PostModel(@NonNull Context context, @Nullable AttributeSet attrs, String title, String body, byte[] profilePhoto, String postUploadDate, String nickname, String numLikes, String media, String mimeType, String uniquePostRef, String userId, ArrayList<String> postLikedBy) {
        super(context, attrs);
        this.title = title;
        this.body = body;
        this.profilePhoto = profilePhoto;
        this.nickname = nickname;
        this.postUploadDate = postUploadDate;
        this.numLikes = numLikes;
        this.media = media;
        this.mimeType = mimeType;
        this.uniquePostRef = uniquePostRef;
        this.userId = userId;
    }

    public PostModel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, String title, String body, byte[] profilePhoto, String nickname, String postUploadDate, String numLikes, String media, String mimeType, String uniquePostRef, String userId, ArrayList<String> postLikedBy) {
        super(context, attrs, defStyleAttr);
        this.title = title;
        this.body = body;
        this.profilePhoto = profilePhoto;
        this.nickname = nickname;
        this.postUploadDate = postUploadDate;
        this.numLikes = numLikes;
        this.media = media;
        this.mimeType = mimeType;
        this.uniquePostRef = uniquePostRef;
        this.userId = userId;
    }

    public ArrayList<String> getPostLikedBy() {
        return postLikedBy;
    }
    public ArrayList<String> getPostDislikedBy() {return postDislikedBy;}

    public void setPostLikedBy(ArrayList<String> postLikedBy) {
        this.postLikedBy = postLikedBy;
    }

    public void setPostDislikedBy(ArrayList<String> postDislikedBy) {this.postDislikedBy = postDislikedBy;}
    public String getUserId() {return userId;}
    public String getUniquePostRef() {
        return uniquePostRef;
    }

    public String getMimeType() {
        return mimeType;
    }
    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public byte[] getProfilePhoto() {
        return profilePhoto;
    }

    public String getNickname() {
        return nickname;
    }

    public String getDatePosted() {
        return postUploadDate;
    }
    public String getNumLikes() {
        return numLikes;
    }
    public String getMedia() {
        return media;
    }

    public void setNumLikes(int numLikes) {
        this.numLikes = String.valueOf(numLikes);
    }

}
