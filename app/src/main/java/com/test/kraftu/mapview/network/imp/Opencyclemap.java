package com.test.kraftu.mapview.network.imp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.test.kraftu.mapview.network.TileResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Opencyclemap implements TileResource {
    private static int TILE_SIZE_X = 256;
    private static int TILE_SIZE_Y = 256;
    private static int COUNT_TILE_X = 100;
    private static int COUNT_TILE_Y = 100;

    private static int TILE_OFFSET_X = 33198;
    private static int TILE_OFFSET_Y = 22539;

    @Override
    public Bitmap loadTile(String link) {
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
        return String.format("http://b.tile.opencyclemap.org/cycle/16/%d/%d.png ",
                TILE_OFFSET_X+tileX,TILE_OFFSET_Y+tileY);
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
}
