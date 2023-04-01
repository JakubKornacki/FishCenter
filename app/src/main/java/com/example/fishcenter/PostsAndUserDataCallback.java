package com.example.fishcenter;

import java.util.ArrayList;

public interface PostsAndUserDataCallback {
    void userDataReady(User user);
    void userPostsReady(ArrayList<PostModel> userPosts);
    void userPostUpdated(int position);
    void newPostSaved(LocalPost newPost);
}
