package com.test.kraftu.mapview.core.imp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.test.kraftu.mapview.cache.DiskCache;
import com.test.kraftu.mapview.cache.imp.BaseDiskCache;
import com.test.kraftu.mapview.cache.imp.LruMemoryCache;
import com.test.kraftu.mapview.cache.MemoryCache;
import com.test.kraftu.mapview.core.TileManager;
import com.test.kraftu.mapview.network.imp.BaseImageDownload;
import com.test.kraftu.mapview.network.TileDownloader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;


public class BaseTileManager implements TileManager {

    private static int TILE_SIZE_X = 256;
    private static int TILE_SIZE_Y = 256;
    private static int COUNT_TILE_X = 20;
    private static int COUNT_TILE_Y = 20;

    private MemoryCache memoryCache;
    private DiskCache diskCache;
    private TileDownloader imageDownload;
    private TileManagerListener tileManagerListener;
    private Executor executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("BaseTileManager");
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    });

    HashMap<Integer,LoadBitmap> listTask = new HashMap<>();

    Handler handler = new Handler(Looper.myLooper());

    public BaseTileManager(Context context) {
        memoryCache = new LruMemoryCache(15*1024*1024);
        imageDownload = new BaseImageDownload();
        diskCache = new BaseDiskCache(new File(context.getCacheDir(),"/imageMap"));
    }

    @Override
    public int getCountTileX() {
        return COUNT_TILE_X;
    }

    @Override
    public int getCountTileY() {
        return COUNT_TILE_Y;
    }

    @Override
    public int getWidthTile() {
        return TILE_SIZE_X;
    }

    @Override
    public int getHeightTile() {
        return TILE_SIZE_Y;
    }

    @Override
    public Bitmap getBitmapTile(int tileX, int tileY) {
        Integer tileId = getTileId(tileX,tileY);
        Bitmap bitmap = memoryCache.get(tileId.toString());
        if(bitmap == null && !listTask.containsKey(tileId)){
            LoadBitmap loadBitmap = new LoadBitmap(tileId,imageDownload.getUriForTile(tileX,tileY));
            listTask.put(tileId,loadBitmap);
            executor.execute(loadBitmap);
            Log.d("BaseTileManager",String.format("Create %s",loadBitmap.toString()));
        }
        return bitmap;
    }

    @Override
    public int getTileId(int tileX, int tileY) {
        return tileY*COUNT_TILE_X + tileX;
    }



    private class LoadBitmap implements Runnable{
        private Integer tileId;
        private String url;
        private boolean isCancel;
        Bitmap bitmap;

        public LoadBitmap(int tileId, String url) {
            this.tileId = tileId;
            this.url = url;
        }

        @Override
        public void run() {
            if(!isCancel) {

                File file = diskCache.get(url);
                if(file.exists()){
                    bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                }

                if(bitmap == null) {
                    bitmap = imageDownload.download(url);
                    if(bitmap!=null){
                        try {
                            diskCache.save(url,bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if(bitmap != null)handler.post(new Runnable() {
                    @Override
                    public void run() {
                        memoryCache.put(tileId.toString(), bitmap);
                        notifyLoedeNewTile(tileId);
                    }
                });
            }else{
                Log.d("BaseTileManager",String.format("Cancel taskid:%d",tileId));
            }
            handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listTask.remove(tileId);
                    }
            });
        }

        public void cancel() {
            isCancel = true;
        }

        @Override
        public String toString() {
            return "LoadBitmap{" +
                    "tileId=" + tileId +
                    ", url='" + url + '\'' +
                    ", isCancel=" + isCancel +
                    ", bitmap=" + bitmap +
                    '}';
        }
    }

    public void setTileManagerListener(TileManagerListener tileManagerListener) {
        this.tileManagerListener = tileManagerListener;
    }

    private void notifyLoedeNewTile(int idTile){
        if(tileManagerListener!=null) tileManagerListener.loadedNewTile(idTile);
    }

    public interface TileManagerListener{
        void loadedNewTile(int idTile);
    }
}
