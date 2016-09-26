package com.teqnihome.cardtransfer;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * This is class for image caching
 * Created by dhiren
 * @author dhiren
 */
public class ImageCache {
    public static Map<String, List<Uri>> requests = new HashMap<>();

    private static WeakHashMap<String, Bitmap> map = new WeakHashMap<>();
    private static String[] URIs;
    private static Context contextS;
    static Uri uriS;
    private static Map<String, Boolean> imageCheckBox = new WeakHashMap<>();


    public static void setImageCheckBoxValue(String str, Boolean value) {
        imageCheckBox.put(str, value);
    }

    public static boolean canShowImage() {
        return imageCheckBox.containsValue(true);
    }


    public static Boolean getImageCheckBox(String string) {
        return imageCheckBox.get(string);
    }


    public static Map<String, Boolean> getImageCheckBox() {
        return imageCheckBox;
    }


    private static Map<String, Boolean> audioCheckBox = new WeakHashMap<>();


    public static void setAudioCheckBoxValue(String str, Boolean value) {
        audioCheckBox.put(str, value);
    }

    public static boolean canShowMusic() {
        return audioCheckBox.containsValue(true);
    }

    public static Boolean getAudioCheckBox(String string) {
        return audioCheckBox.get(string);
    }

    public static Map<String, Boolean> getAudioCheckBox() {
        return audioCheckBox;
    }


    private static Map<String, Boolean> videoCheckBox = new WeakHashMap<>();


    public static void setVideoCheckBoxValue(String str, Boolean value) {
        videoCheckBox.put(str, value);
    }

    public static Boolean getVideoCheckBox(String string) {
        return videoCheckBox.get(string);
    }

    public static boolean canShowVideo() {
        return videoCheckBox.containsValue(true);
    }

    public static Map<String, Boolean> getVideoCheckBox() {
        return videoCheckBox;
    }


    public static void setURIs(String[] URI) {
        URIs = URI;
    }

    public static String[] getURIs() {
        return URIs;
    }

    public static void put(String key, Bitmap val) {
        map.put(key, val);
    }

    public static Bitmap get(String key) {
        return map.get(key);
    }

    public static void remove(String key) {
        map.remove(key);
    }

    public static void setContext(Context context) {
        contextS = context;

    }

    public static Context getContext() {
        return contextS;

    }


    public static void setUri(Uri uri) {
        uriS = uri;
    }

    public static Uri getUri() {
        return uriS;
    }

    public static List<Uri> getUriList(String deviceAddress) {
        return requests.get(deviceAddress);
    }

    public static void putUri(String deviceAddress, Uri uri) {
        if (requests.containsKey(deviceAddress)) {
            List<Uri> urilist = requests.get(deviceAddress);
            urilist.add(uri);
            requests.put(deviceAddress, urilist);
        } else {
            List<Uri> uriList = new ArrayList<>();
            uriList.add(uri);
            requests.put(deviceAddress, uriList);
        }
    }


    static Map<String, List<String>> deleteImages = new HashMap<>();

    private static final String TAG = "ImageCache";
    public static void addDeleteImage(String type, String str) {
        Log.d(TAG, "addDeleteImage: "+str);
        if (deleteImages.containsKey(type) && deleteImages.get(type).size() > 0 ) {
            List<String> temp = deleteImages.get(type);
            temp.add(str);
            deleteImages.put(type, temp);
        } else {
            List<String> arrayList = new ArrayList<>();
            arrayList.add(str);
            deleteImages.put(type, arrayList);
        }
    }

    public static void removeImages(String type, String str) {
        deleteImages.get(type).remove(str);
    }

    public static Map<String, List<String>> getDeleteImages() {
        return deleteImages;
    }
}
