package com.example.fishcenter;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {LocalPost.class}, version = 1)
public abstract class PostsDatabase extends RoomDatabase {
    public abstract PostsDao postsDao();
}
