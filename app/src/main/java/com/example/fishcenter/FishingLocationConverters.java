package com.example.fishcenter;
import androidx.room.TypeConverter;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
// https://developer.android.com/training/data-storage/room/referencing-data
// converters class is necessary to instruct the ROOM database how to store data types other than primitives
// ROOM database can store boxed types, therefore, Double[] values is perfectly valid and is equivalent to double[]

public class FishingLocationConverters {
    @TypeConverter
    public ArrayList<Double> fromString(String gsonValues) {
        Gson gson = new Gson();
        // tells Gson to convert the JSON string into an ArrayList of type Double
        TypeToken<ArrayList<Double>> typeToken = new TypeToken<ArrayList<Double>>() {};
        // get the type object out of the created type token and use it to
        // tell Gson that the JSON string should be parsed into an object of type ArrayList<Double>
        Type arrayListDoubleType = typeToken.getType();
        return gsonValues == null ? null : gson.fromJson(gsonValues, arrayListDoubleType);
    }
    @TypeConverter
    public String fromArrayList(ArrayList<Double> values) {
        Gson gson = new Gson();
        // convert the ArrayList of type Double to a JSON string
        return values == null ? null : gson.toJson(values);
    }
}

