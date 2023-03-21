package com.example.fishcenter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.ArrayList;
@Entity(tableName = "LocalPosts")
public class LocalPost implements Serializable {
    @NonNull
    @ColumnInfo(name = "title")
    private String title;
    @NonNull
    @ColumnInfo(name = "body")
    private String body;
    @NonNull
    @ColumnInfo(name = "profilePhoto")
    private byte[] profilePhoto;
    @NonNull
    @ColumnInfo(name = "nickname")
    private String nickname;
    @NonNull
    @ColumnInfo(name = "postUploadDate")
    private String postUploadDate;
    @Nullable
    @ColumnInfo(name = "numLikes")
    private String numLikes;
    @Nullable
    @ColumnInfo(name = "numDislikes")
    private String numDislikes;
    @Nullable
    @ColumnInfo(name = "media")
    private String media;
    @Nullable
    @ColumnInfo(name = "mimeType")
    private String mimeType;
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "uniquePostRef")
    private String uniquePostRef;
    @NonNull
    @ColumnInfo(name = "userId")
    private String userId;
    public LocalPost(String title, String body, byte[] profilePhoto, String nickname, String postUploadDate, String numLikes, String numDislikes, String media, String mimeType, String uniquePostRef, String userId) {
        this.title = title;
        this.body = body;
        this.profilePhoto = profilePhoto;
        this.nickname = nickname;
        this.postUploadDate = postUploadDate;
        this.numLikes = numLikes;
        this.numDislikes = numDislikes;
        this.media = media;
        this.mimeType = mimeType;
        this.uniquePostRef = uniquePostRef;
        this.userId = userId;
    }

    @NonNull
    public String getTitle() {
        return title;
    }
    @NonNull
    public String getBody() {
        return body;
    }
    @NonNull
    public byte[] getProfilePhoto() {
        return profilePhoto;
    }
    @NonNull
    public String getNickname() {
        return nickname;
    }
    @NonNull
    public String getPostUploadDate() {
        return postUploadDate;
    }
    @Nullable
    public void setPostUploadDate(String postUploadDate) {
        this.postUploadDate = postUploadDate;
    }
    @Nullable
    public String getNumLikes() {
        return numLikes;
    }

    @Nullable
    public String getMedia() {
        return media;
    }

    @Nullable
    public String getMimeType() {
        return mimeType;
    }

    @NonNull
    public String getUniquePostRef() {
        return uniquePostRef;
    }
    @NonNull
    public String getUserId() {
        return userId;
    }

    public String getNumDislikes() {
        return numDislikes;
    }

    public void setNumLikes(String numLikes) {
        this.numLikes = numLikes;
    }

    public void setNumDislikes(String numDislikes) {
        this.numDislikes = numDislikes;
    }
}
