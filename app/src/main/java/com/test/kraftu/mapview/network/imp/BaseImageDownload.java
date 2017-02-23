package com.test.kraftu.mapview.network.imp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.test.kraftu.mapview.network.TileDownloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Слава on 23.02.2017.
 */

public class BaseImageDownload implements TileDownloader {
    @Override
    public Bitmap download(String link) {
        try {
            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            // Log exception
            return null;
        }
    }
    @Override
    public String getUriForTile(int tileX, int tileY) {
        return String.format("http://b.tile.opencyclemap.org/cycle/16/%d/%d.png ",33198+tileX,22539+tileY);
    }
}
