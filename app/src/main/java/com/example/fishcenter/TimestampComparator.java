package com.example.fishcenter;

import java.util.Comparator;

public class TimestampComparator implements Comparator<PostModel> {
    @Override
    public int compare(PostModel post1, PostModel post2) {
        // sort the posts in ascending order based on the timestamp value
        return post2.getDatePosted().compareTo(post1.getDatePosted());
    }
}