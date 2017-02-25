package com.test.kraftu.mapview.cache.imp;

import android.graphics.Bitmap;

import com.test.kraftu.mapview.cache.MemoryCache;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;


public class LastUsageMemoryCache implements MemoryCache {
    private final LinkedHashMap<Integer,OverBitmap> map;
    private final TreeMap<Long,Integer> mapLastUsage;
    private final int maxSize;
    private int size;

    public LastUsageMemoryCache(int maxSize) {
        if(maxSize < 0)  throw new IllegalArgumentException("maxSize <= 0");
        this.maxSize = maxSize;
        map = new LinkedHashMap<>();
        mapLastUsage = new TreeMap<>(new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                return o1.compareTo(o2);
            }
        });
    }

    @Override
    public boolean put(Integer key, Bitmap value) {
        if (key == null || value == null) {
            throw new NullPointerException("key or value is null");
        }

        synchronized (this) {
            size += sizeOf(value);
            OverBitmap overBitmap = new OverBitmap(value,System.nanoTime());
            mapLastUsage.put(overBitmap.lastTimeUsage,key);
            overBitmap = map.put(key, overBitmap);
            if (overBitmap != null) {
                size -= sizeOf(overBitmap.bitmap);
            }
        }
        trimToSize(maxSize);
        return true;
    }

    @Override
    public Bitmap get(Integer key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        synchronized (this) {
            OverBitmap overBitmap = map.get(key);
            if(overBitmap != null){
                mapLastUsage.remove(overBitmap.lastTimeUsage);
                overBitmap.lastTimeUsage = System.nanoTime();
                mapLastUsage.put(overBitmap.lastTimeUsage,key);
            }
            return overBitmap != null ? overBitmap.bitmap : null;
        }
    }

    @Override
    public Bitmap remove(Integer key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        synchronized (this) {
            OverBitmap previous = map.remove(key);
            if (previous != null) {
                size -= sizeOf(previous.bitmap);
                previous.bitmap.recycle();
                previous.bitmap = null;
                mapLastUsage.remove(previous.lastTimeUsage);
            }
            return null;
        }
    }

    private void trimToSize(int maxSize) {
        while (true) {
            Integer key;
            OverBitmap value;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(getClass().getName() + ".sizeOf() is reporting inconsistent results!");
                }

                if (size <= maxSize || map.isEmpty()) {
                    break;
                }


                Map.Entry<Long, Integer> toEvict = mapLastUsage.firstEntry();
                if (toEvict == null) {
                    break;
                }
                key = toEvict.getValue();
                value = map.get(key);
                map.remove(key);
                size -= sizeOf(value.bitmap);
                mapLastUsage.remove(toEvict.getKey());
                if(value.bitmap != null){
                    value.bitmap.recycle();
                    value.bitmap = null;
                }
            }
        }
    }

    @Override
    public void clear() {
        trimToSize(1);
    }

    private int sizeOf(Bitmap value) {
        return value.getRowBytes() * value.getHeight();
    }

    @Override
    public String toString() {
        return "LruMemoryCache{" +
                "maxSize=" + maxSize +
                ", size=" + size +
                '}';
    }

    public class OverBitmap{
        public Bitmap bitmap;
        public long lastTimeUsage;

        public OverBitmap(Bitmap bitmap, long lastTimeUsage) {
            this.bitmap = bitmap;
            this.lastTimeUsage = lastTimeUsage;
        }
    }
}