package com.example.fishcenter;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class PostModel extends RecyclerView {
    private String title;
    private String body;
    private byte[] profilePhoto;
    private String nickname;
    private String postUploadDate;
    private String numLikes;
    private Uri media;


    public PostModel(@NonNull Context context) {
        super(context);
    }

    public PostModel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PostModel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PostModel(@NonNull Context context, String title, String body, byte[] profilePhoto, String nickname, String postUploadDate, String numLikes, Uri media) {
        super(context);
        this.title = title;
        this.body = body;
        this.profilePhoto = profilePhoto;
        this.nickname = nickname;
        this.postUploadDate = postUploadDate;
        this.numLikes = numLikes;
        this.media = media;
    }

    public PostModel(@NonNull Context context, @Nullable AttributeSet attrs, String title, String body, byte[] profilePhoto, String postUploadDate, String nickname, String numLikes, Uri media) {
        super(context, attrs);
        this.title = title;
        this.body = body;
        this.profilePhoto = profilePhoto;
        this.nickname = nickname;
        this.postUploadDate = postUploadDate;
        this.numLikes = numLikes;
        this.media = media;
    }

    public PostModel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, String title, String body, byte[] profilePhoto, String nickname, String postUploadDate, String numLikes, Uri media) {
        super(context, attrs, defStyleAttr);
        this.title = title;
        this.body = body;
        this.profilePhoto = profilePhoto;
        this.nickname = nickname;
        this.postUploadDate = postUploadDate;
        this.numLikes = numLikes;
        this.media = media;
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

    public Uri getMedia() {
        return media;
    }
}
