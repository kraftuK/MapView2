package com.test.kraftu.mapview.cache;

import android.graphics.Bitmap;


public interface MemoryCache{
    boolean put(String key,Bitmap value);
    Bitmap get(String key);
    Bitmap remove(String key);
    void clear();
}
