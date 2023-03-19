package com.example.fishcenter;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Base64;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FishImage {
    private Uri fishImage;
    private String imageFileMimeType;
    private String imageFileName;
    private String imageFileSize;
    private byte[] imageFileBytesArray;
    private byte[] imageFileBytesArrayMD5;
    private String imageFileBytesArrayMD5EncodedBase64;
    private ContentResolver contentResolver;

    public FishImage(Uri fishImage, ContentResolver contentResolver) {
        this.fishImage = fishImage;
        this.contentResolver = contentResolver;
        imageFileMimeType = MediaUtilities.extractMediaMimeType(fishImage, contentResolver);
        imageFileName = extractImageFileName();
        imageFileSize = extractImageFileSize();
        imageFileBytesArray = getImageFileBytesArray();
        imageFileBytesArrayMD5 = getImageBytesMD5Checksum();
        imageFileBytesArrayMD5EncodedBase64 = encodeImageBytesArrayToBase64();
    }

    private String extractImageFileName() {
        // create a cursor object to point at the image data that is returned by the content resolver query which is supplied with the image uri
        Cursor cursor = contentResolver.query(fishImage, null, null, null, null);
        // move the first element initially it points to 0 which points to no element now the cursor is able to fetch the data wanted when queried with the column index
        cursor.moveToFirst();
        // get the index of the column which contains the display name of the file
        int fileNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        // get the element at the column with index fileNameIndex
        return cursor.getString(fileNameIndex);
    }


    private String extractImageFileSize() {
        // create a cursor object to point at the image data that is returned by the content resolver query which is supplied with the image uri
        Cursor cursor = contentResolver.query(fishImage, null, null, null, null);
        // move the first element initially it points to 0 which points to no element now the cursor is able to fetch the data wanted when queried with the column index
        cursor.moveToFirst();
        // get the index of the column which contains the size of the file
        int imageSizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
        // get the element at the column with index imageSizeIndex
        return cursor.getString(imageSizeIndex);
    }

    private byte[] getImageFileBytesArray() {
        try {
            int noOfBytes = Integer.parseInt(imageFileSize);
            byte[] byteArray = new byte[noOfBytes];
            // open an input stream using the content resolver and the image uri
            InputStream inputStream = contentResolver.openInputStream(fishImage);
            // byte array to hold raw image data of the same size as the image
            // read bytes of the image into the byte array
            inputStream.read(byteArray);
            // close the input stream
            inputStream.close();
            return byteArray;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // below implementation is based on the information found on GeeksForGeeks https://www.geeksforgeeks.org/md5-hash-in-java/?ref=rp
    private byte[] getImageBytesMD5Checksum() {
        try {
            byte[] md5DigestedBytes;
            // parse the file size in bytes to int
            int noOfBytes = Integer.parseInt(imageFileSize);
            // open an input stream using the content resolver and the image uri
            InputStream inputStream = contentResolver.openInputStream(fishImage);
            // byte array to hold raw image data of the same size as the image
            byte[] byteArray = new byte[noOfBytes];
            // read bytes of the image into the byte array
            inputStream.read(byteArray);
            // close the input stream
            inputStream.close();
            // get an instance of the MD5 hash alg.
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            // get bytes digested by MD5
            md5DigestedBytes = md5.digest(byteArray);
            return md5DigestedBytes;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String encodeImageBytesArrayToBase64() {
        return Base64.encodeToString(imageFileBytesArrayMD5, Base64.DEFAULT);
    }

    public Uri getFishImage() {
        return fishImage;
    }

    public String getImageFileMimeType() {
        return imageFileMimeType;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public String getImageFileSize() {
        return imageFileSize;
    }

    public String getImageFileBytesArrayMD5EncodedBase64() {
        return imageFileBytesArrayMD5EncodedBase64;
    }
    public ContentResolver getContentResolver(){
        return contentResolver;
    }


}
