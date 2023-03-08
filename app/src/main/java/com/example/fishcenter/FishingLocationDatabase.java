package com.example.fishcenter;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {FishingLocation.class}, version = 1)
public abstract class FishingLocationDatabase extends RoomDatabase {
    public abstract FishingLocationDao fishingLocationDao();
}
