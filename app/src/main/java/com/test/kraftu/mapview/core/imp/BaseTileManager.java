package com.test.kraftu.mapview.core.imp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.test.kraftu.mapview.cache.DiskCache;
import com.test.kraftu.mapview.cache.imp.BaseDiskCache;
import com.test.kraftu.mapview.cache.imp.BaseMemoryCache;
import com.test.kraftu.mapview.cache.MemoryCache;
import com.test.kraftu.mapview.core.TileManager;
import com.test.kraftu.mapview.core.TileManagerListener;
import com.test.kraftu.mapview.network.imp.Opencyclemap;
import com.test.kraftu.mapview.network.TileResource;
import com.test.kraftu.mapview.utils.MapThreadFactory;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class BaseTileManager implements TileManager {
    public static final int THREAD_POOL_SIZE = 3;
    public static final int SIZE_MEMORY_CACHE = 30 * 1024 * 1024;

    private Handler handler = new Handler(Looper.myLooper());

    private MemoryCache memoryCache;

    private DiskCache diskCache;

    private TileResource tileRes;

    private Reference<TileManagerListener> tileListenerRef;

    private Executor executor =
            Executors.newFixedThreadPool(THREAD_POOL_SIZE,new MapThreadFactory("BaseTileManager"));
    private HashMap<Integer,LoadBitmap> listTask = new HashMap<>();

    public BaseTileManager(Context context) {
        tileRes = new Opencyclemap();
        memoryCache = new BaseMemoryCache(SIZE_MEMORY_CACHE);
        diskCache = new BaseDiskCache(context.getCacheDir());
    }

    @Override
    public Bitmap getBitmapTile(int tileX, int tileY) {
        Integer tileId = getTileId(tileX,tileY);
        Bitmap bitmap = memoryCache.get(tileId.toString());
        if(bitmap == null && !listTask.containsKey(tileId)){
            LoadBitmap loadBitmap = new LoadBitmap(tileId, tileRes.getUriForTile(tileX,tileY));
            listTask.put(tileId,loadBitmap);
            executor.execute(loadBitmap);
            Log.d("BaseTileManager",String.format("Create %s",loadBitmap.toString()));
        }
        return bitmap;
    }

    @Override
    public int getTileId(int tileX, int tileY) {
        return tileY * tileRes.getCountTileX() + tileX;
    }

    @Override
    public TileResource getTileDownloader() {
        return tileRes;
    }

    public void setTileManagerListener(TileManagerListener tileManagerListener) {
        this.tileListenerRef = new WeakReference<>(tileManagerListener);
    }

    @Override
    public void cuncelLoad() {
        for(Map.Entry<Integer,LoadBitmap> item : listTask.entrySet()){
            item.getValue().cancelTask();
        }
    }

    @Override
    public void clearCache() {
        if(memoryCache != null) memoryCache.clear();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if(diskCache != null) diskCache.clear();
            }
        });
    }

    private void notifyLoedeNewTile(int idTile){
        TileManagerListener listener = tileListenerRef.get();
        if(listener!=null) listener.loadedNewTile(idTile);
    }

    private class LoadBitmap implements Runnable{
        private static final String TAG = "LoadBitmap";
        private Integer tileId;
        private String url;
        private boolean isCancel;
        private Bitmap mBitmap;

        public LoadBitmap(int tileId, String url) {
            this.tileId = tileId;
            this.url = url;
        }
        @Override
        public void run() {
            try {
                checkCancel();
                //Try loadTile from sd card cache

                if (diskCache != null) {
                    File file = diskCache.get(url);
                    if (file.exists()) {
                        mBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                        Log.d(TAG, String.format("id:%d from fromSd:%b",
                                tileId, mBitmap!=null));
                    }
                }
                checkCancel();

                //Try loadTile from tileSource and save sd cache
                if (mBitmap == null) {
                    mBitmap = tileRes.loadTile(url);
                    Log.d(TAG, String.format("id:%d from tileRes:%b",
                            tileId, mBitmap!=null));
                    if (mBitmap != null && diskCache != null) {
                        diskCache.save(url, mBitmap);
                    }
                }

                if (mBitmap != null)
                    addCacheMemotyAndNotify(tileId,mBitmap);
            }catch (Exception e){
                Log.e(TAG, String.format("id:%d exc::%s", tileId, e.getMessage()));
            }finally {
                loadedFinish();
            }

        }
        private void addCacheMemotyAndNotify(final Integer id,final Bitmap bitmap){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    memoryCache.put(id.toString(), bitmap);
                    notifyLoedeNewTile(id);
                }
            });
        }

        private void loadedFinish(){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listTask.remove(tileId);
                }
            });
        }
        private void checkCancel() throws Exception{
            if(isCancel) throw new Exception("TaskCancel");
        }

        public void cancelTask() {
            isCancel = true;
        }
        @Override
        public String toString() {
            return "LoadBitmap{" +
                    "tileId=" + tileId +
                    ", url='" + url + '\'' +
                    ", isCancel=" + isCancel +
                    ", bitmap=" + mBitmap +
                    '}';
        }
    }
}
