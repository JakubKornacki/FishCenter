package com.example.fishcenter;

import java.util.ArrayList;

public interface PostsCallback {
    void userPostsReady(ArrayList<PostModel> userPosts);
    void userPostUpdated(int position);
    void newPostSaved(LocalPost newPost);
    void isSynchronisationNecessary(boolean isNecessary);
}
