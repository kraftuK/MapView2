package com.test.kraftu.mapview.network;

import android.graphics.Bitmap;

public interface TileDownloader {
    Bitmap download(String url);
    String getUriForTile(int tileX,int tileY);
}
