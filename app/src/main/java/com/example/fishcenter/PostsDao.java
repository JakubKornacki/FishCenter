package com.example.fishcenter;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PostsDao {
    @Query("SELECT * FROM LocalPosts")
    List<LocalPost> getAllLocalPosts();
    @Insert
    void addLocalPost(LocalPost localPost);
    @Update
    void updateLocalPost(LocalPost localPost);
    @Delete
    void deleteLocalPost(LocalPost localPost);

    @Query("SELECT * FROM LocalPosts WHERE uniquePostRef  == :uniquePostRefToCheck")
    LocalPost findLocalPostByUniqueRef(String uniquePostRefToCheck);
}
