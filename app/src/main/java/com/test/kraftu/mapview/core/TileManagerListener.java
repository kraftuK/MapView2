package com.test.kraftu.mapview.core;


public interface TileManagerListener {
    void loadedTile(int idTile);

    void errorTile(int idTile, Exception exc);
}