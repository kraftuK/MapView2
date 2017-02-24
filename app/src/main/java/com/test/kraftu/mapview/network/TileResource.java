package com.test.kraftu.mapview.network;

import android.graphics.Bitmap;

public interface TileResource {
    Bitmap loadTile(String url);
    String getUriForTile(int tileX,int tileY);
    int getCountTileX();
    int getCountTileY();
    int getWidthTile();
    int getHeightTile();
}
