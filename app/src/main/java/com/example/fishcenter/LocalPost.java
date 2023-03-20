package com.example.fishcenter;

import java.io.Serializable;
import java.util.ArrayList;

public class LocalPost implements Serializable {
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
    public LocalPost(String title, String body, byte[] profilePhoto, String nickname, String postUploadDate, String numLikes, String media, String mimeType, String uniquePostRef, String userId, ArrayList<String> postLikedBy, ArrayList<String> postDislikedBy) {
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
        this.postLikedBy = postLikedBy;
        this.postDislikedBy = postDislikedBy;
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
    public String getPostUploadDate() {
        return postUploadDate;
    }

    public void setPostUploadDate(String postUploadDate) {
        this.postUploadDate = postUploadDate;
    }

    public String getNumLikes() {
        return numLikes;
    }

    public String getMedia() {
        return media;
    }


    public String getMimeType() {
        return mimeType;
    }


    public String getUniquePostRef() {
        return uniquePostRef;
    }


    public String getUserId() {
        return userId;
    }

    public ArrayList<String> getPostLikedBy() {
        return postLikedBy;
    }

    public ArrayList<String> getPostDislikedBy() {
        return postDislikedBy;
    }

}
