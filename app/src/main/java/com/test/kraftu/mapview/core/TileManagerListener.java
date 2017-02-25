package com.test.kraftu.mapview.core;


public interface TileManagerListener {
    void loadedTile(int idTile);

    void erorrTile(int idTile, Exception exc);
}