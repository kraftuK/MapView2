package com.test.kraftu.mapview.core.imp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.test.kraftu.mapview.cache.DiskCache;
import com.test.kraftu.mapview.cache.MemoryCache;
import com.test.kraftu.mapview.cache.imp.LastUsageMemoryCache;
import com.test.kraftu.mapview.cache.imp.LruMemoryCache;
import com.test.kraftu.mapview.cache.imp.UnlimitedDiskCache;
import com.test.kraftu.mapview.core.TileManager;
import com.test.kraftu.mapview.core.TileManagerListener;
import com.test.kraftu.mapview.network.imp.OpencyclemapTileRes;
import com.test.kraftu.mapview.network.TileResource;
import com.test.kraftu.mapview.utils.MapThreadFactory;

import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class BaseTileManager implements TileManager {
    public static final int THREAD_POOL_SIZE = 2;
    public static final int SIZE_MEMORY_CACHE = 15 * 1024 * 1024;
    public static final boolean DEBUG = false;
    public static final String TAG = "BaseTileManager";

    private Handler mHandler = new Handler(Looper.myLooper());

    private MemoryCache mMemoryCache;

    private DiskCache mDiskCache;

    private TileResource mTileRes;

    private Reference<TileManagerListener> mTileListenerRef;

    private ExecutorService mExecutor;
    private HashMap<Integer,LoadBitmap> mListTask = new HashMap<>();

    public BaseTileManager(Context context) {
        mTileRes = new OpencyclemapTileRes();
        mMemoryCache = new LastUsageMemoryCache(SIZE_MEMORY_CACHE);
        //mDiskCache = new UnlimitedDiskCache(context.getCacheDir());
        mExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE,
                new MapThreadFactory(TAG + "_Thread"));
    }

    @Override
    public Bitmap getBitmapTile(int tileX, int tileY) {
        Integer tileId = getTileId(tileX,tileY);
        Bitmap bitmap = mMemoryCache.get(tileId);

        if(bitmap == null && !mListTask.containsKey(tileId)){
            startLoadTask(tileX,tileY);
        }

        return bitmap;
    }

    private void startLoadTask(int tileX, int tileY){
        Integer tileId = getTileId(tileX,tileY);
        LoadBitmap loadBitmap = new LoadBitmap(tileId,tileX,tileY);
        mListTask.put(tileId,loadBitmap);
        mExecutor.submit(loadBitmap);
        if(DEBUG) Log.d(TAG,String.format("Create %s",loadBitmap.toString()));
    }

    @Override
    public int getTileId(int tileX, int tileY) {
        return tileY * mTileRes.getCountTileX() + tileX;
    }

    @Override
    public TileResource getTileDownloader() {
        return mTileRes;
    }

    public void setTileManagerListener(TileManagerListener tileManagerListener) {
        this.mTileListenerRef = new WeakReference<>(tileManagerListener);
    }

    @Override
    public void cancelLoad() {
        if(DEBUG) Log.d(TAG,String.format("cancelLoad %d",mListTask.size()));

        for(Map.Entry<Integer,LoadBitmap> item : mListTask.entrySet()){
            item.getValue().cancelTask();
        }
    }

    @Override
    public void clearCache() {
        if(DEBUG) Log.d(TAG,String.format("clear cache"));
        if(mMemoryCache != null) mMemoryCache.clear();

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if(mDiskCache != null) mDiskCache.clear();
            }
        });
    }

    private void notifyLoadedNewTile(int idTile){
        TileManagerListener listener = mTileListenerRef.get();
        if(listener != null) listener.loadedTile(idTile);
    }

    private class LoadBitmap implements Runnable{
        private static final String TAG = "LoadBitmap";
        private Integer tileId;
        private Integer tileX;
        private Integer tileY;
        private String url;
        private boolean isCancel;
        private Bitmap mBitmap;

        public LoadBitmap(int tileId, int tileX, int tileY) {
            this.tileId = tileId;
            this.tileX = tileX;
            this.tileY = tileY;
        }
        @Override
        public void run() {
            try {

                checkCancel();
                //Try loadTile from sd card cache
                url = mTileRes.getUriForTile(tileX,tileY);

                if (mDiskCache != null) {
                    File file = mDiskCache.get(url);
                    if (file.exists()) {
                        mBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                        if(DEBUG)Log.d(TAG, String.format("id:%d from fromSd:%b",
                                tileId, mBitmap!=null));
                    }
                }
                checkCancel();

                //Try loadTile from tileSource and save sd cache
                if (mBitmap == null) {
                    mBitmap = mTileRes.loadTile(url);
                    if(DEBUG)Log.d(TAG, String.format("id:%d from mTileRes:%b",
                            tileId, mBitmap!=null));
                    if (mBitmap != null && mDiskCache != null) {
                        mDiskCache.save(url, mBitmap);
                    }
                }

                if (mBitmap != null)
                    addCacheTileAndNotify(tileId,mBitmap);
            }catch (Exception e){
                if(DEBUG)Log.e(TAG, String.format("id:%d exc::%s", tileId, e.getMessage()));
            }finally {
                loadedFinish();
            }

        }
        private void addCacheTileAndNotify(final Integer id, final Bitmap bitmap){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mMemoryCache.put(id, bitmap);
                    notifyLoadedNewTile(id);
                }
            });
        }

        private void loadedFinish(){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListTask.remove(tileId);
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
