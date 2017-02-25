package com.test.kraftu.mapview.core;


import android.graphics.Bitmap;

import com.test.kraftu.mapview.network.TileResource;

public interface TileManager {

    void updateVisibleTile(int tileX,int tileY,int sizeX,int sizeY);
    Bitmap getBitmapTile(int tileX, int tileY);
    int getTileId(int tileX, int tileY);
    TileResource getTileDownloader();
    void setTileManagerListener(TileManagerListener listener);
    void cancelLoad();
    void clearCache();
}
