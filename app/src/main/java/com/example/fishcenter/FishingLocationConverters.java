package com.example.fishcenter;
import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;
// https://developer.android.com/training/data-storage/room/referencing-data
// converters class is necessary to instruct the ROOM database how to store data types other than primitives
// ROOM database can store boxed types, therefore, Double[] values is perfectly valid and is equivalent to double[]

public class FishingLocationConverters {
    @TypeConverter
    public ArrayList<Double> fromString(String values) {
        if(values == null) {
            return null;
        }
        ArrayList<Double> doubles = new ArrayList<>();
        String[] valuesSplit = values.split(",");
        // last cell will be empty can only loop up to length-1
        for(int i = 0; i < valuesSplit.length-1; i++) {
            doubles.add(Double.valueOf(valuesSplit[i]));
        }
        return doubles;
    }
    @TypeConverter
    public String fromArrayList(ArrayList<Double> values) {
        if(values == null) {
            return null;
        }
        StringBuilder doubles = new StringBuilder();
        for(int i = 0; i < values.size(); i++) {
            doubles.append(values.get(i)).append(",");
        }
        return doubles.toString();
    }
}

