package com.test.kraftu.mapview.core;


import android.graphics.Bitmap;

public interface TileManager {

    int getCountTileX();
    int getCountTileY();
    int getWidthTile();
    int getHeightTile();

    Bitmap getBitmapTile(int tileX, int tileY);
    int getTileId(int tileX, int tileY);
}
