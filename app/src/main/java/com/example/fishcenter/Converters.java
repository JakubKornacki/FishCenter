package com.example.fishcenter;
import androidx.room.TypeConverter;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.util.ArrayList;
// https://developer.android.com/training/data-storage/room/referencing-data
// converters class is necessary to instruct the ROOM database how to store data types other than primitives
// ROOM database can store boxed types, therefore, Double[] values is perfectly valid and is equivalent to double[]

public class Converters {
    @TypeConverter
    public ArrayList<Double> fromString(String gsonValues) {
        Gson gson = new Gson();
        return gsonValues == null ? null : gson.fromJson(gsonValues, new TypeToken<ArrayList<Double>>(){}.getType());
    }
    @TypeConverter
    public String fromArrayList(ArrayList<Double> values) {
        Gson gson = new Gson();
        return values == null ? null : gson.toJson(values);
    }
}

