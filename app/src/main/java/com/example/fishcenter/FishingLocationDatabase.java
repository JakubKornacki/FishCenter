package com.example.fishcenter;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {FishingLocation.class}, version = 1)
@TypeConverters ({Converters.class})
public abstract class FishingLocationDatabase extends RoomDatabase {
    public abstract FishingLocationDao fishingLocationDao();
}
