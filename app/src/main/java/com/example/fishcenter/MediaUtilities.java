package com.example.fishcenter;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;

public class MediaUtilities {
    // list of supported image MIME types including GIF
    public final static HashSet<String> supportedImageMimeTypes = new HashSet<>(Arrays.asList("image/jpg","image/jpeg","image/png","image/gif"));
    // list of supported video MIME types
    public final static HashSet<String> supportedVideoMimeTypes = new HashSet<>(Arrays.asList("video/3gp","video/mov","video/avi","video/wmv","video/mp4","video/mpeg"));
    public static String getMediaMimeType(ContentResolver contentResolver, Uri media) {
        return media == null ? null : contentResolver.getType(media);
    }

    // below implementation is based on the information found on GeeksForGeeks https://www.geeksforgeeks.org/md5-hash-in-java/?ref=rp
    public static byte[] getMediaMD5Checksum(ContentResolver contentResolver, Uri media) throws NoSuchAlgorithmException, IOException {
        byte[] md5DigestedBytes;
        // parse the file size in bytes to int
        int noOfBytes = getMediaFileSizeInBytes(contentResolver, media);
        // open an input stream using the content resolver and the image uri
        InputStream inputStream = contentResolver.openInputStream(media);
        // byte array to hold raw image data of the same size as the image
        byte[] byteArray = new byte[noOfBytes];
        // read bytes of the image into the byte array
        int numberBytesRead = inputStream.read(byteArray);
        if(numberBytesRead != noOfBytes) {
            Log.w(TAG, "MediaUtilities: Media not read in fully!");
            throw new IOException();
        }
        // close the input stream
        inputStream.close();
        // get an instance of the MD5 hash algorithm
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        // get bytes digested by MD5
        md5DigestedBytes = md5.digest(byteArray);
        return md5DigestedBytes;
    }

    public static int getMediaFileSizeInBytes(ContentResolver contentResolver, Uri media) {
        // create a cursor object to point at the image data that is returned by the content resolver query which is supplied with the image uri
        Cursor cursor = contentResolver.query(media, null, null, null, null);
        // move the first element initially it points to 0 which points to no element now the cursor is able to fetch the data wanted when queried with the column index
        cursor.moveToFirst();
        // get the index of the column which contains the size of the file
        int imageSizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
        // get the element at the column with index imageSizeIndex
        int fileSizeInBytes = Integer.parseInt(cursor.getString(imageSizeIndex));
        cursor.close();
        return fileSizeInBytes;
    }

    public static String getMediaFileName(ContentResolver contentResolver, Uri media) {
        // create a cursor object to point at the image data that is returned
        // by the content resolver query which is supplied with the image uri
        Cursor cursor = contentResolver.query(media, null, null, null, null);
        // move the first element initially it points to 0 which points to no element now
        // the cursor is able to fetch the data wanted when  queried with the column index
        cursor.moveToFirst();
        // get the index of the column which contains the display name of the file
        int fileNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        // get the element at the column with index fileNameIndex
        String fileName = cursor.getString(fileNameIndex);
        cursor.close();
        return fileName;
    }

    public static byte[] getImageFileBytes(ContentResolver contentResolver, Uri media) throws IOException {
        int noOfBytes = getMediaFileSizeInBytes(contentResolver, media);
        byte[] byteArray = new byte[noOfBytes];
        // open an input stream using the content resolver and the image uri
        InputStream inputStream = contentResolver.openInputStream(media);
        // byte array to hold raw image data of the same size as the image
        // read bytes of the image into the byte array
        int numberBytesRead = inputStream.read(byteArray);
        if(numberBytesRead != noOfBytes) {
            Log.w(TAG, "MediaUtilities: Media not read in fully!");
            throw new IOException();
        }
        // close the input stream
        inputStream.close();
        return byteArray;
    }

    public static String getBase64EncodedMedia(byte[] mediaBytes) {
        return Base64.encodeToString(mediaBytes, Base64.DEFAULT);
    }


}