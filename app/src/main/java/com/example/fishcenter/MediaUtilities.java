package com.example.fishcenter;

import android.content.ContentResolver;
import android.net.Uri;

import java.util.Arrays;
import java.util.HashSet;

public class MediaUtilities {
    // list of supported image MIME types including GIF
    public final static HashSet<String> supportedImageMimeTypes = new HashSet<>(Arrays.asList("image/jpg","image/jpeg","image/png","image/gif"));
    // list of supported video MIME types
    public final static HashSet<String> supportedVideoMimeTypes = new HashSet<>(Arrays.asList("video/3gp","video/mov","video/avi","video/wmv","video/mp4","video/mpeg"));
    public static String extractMediaMimeType(Uri media, ContentResolver contResolver) {
        return media == null ? null : contResolver.getType(media);
    }
}