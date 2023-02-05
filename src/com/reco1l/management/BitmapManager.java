package com.reco1l.management;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.reco1l.global.Game;
import com.reco1l.interfaces.ITextures;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Created by Reco1l on 22/8/22 21:38

public class BitmapManager {

    public static final BitmapManager instance = new BitmapManager();

    private final Map<String, Bitmap> bitmaps;

    //--------------------------------------------------------------------------------------------//

    public BitmapManager() {
        bitmaps = new HashMap<>();
    }

    //--------------------------------------------------------------------------------------------//

    public InputStream tryGetAsset(String file) {
        try {
            return Game.activity.getAssets().open("gfx/" + file);
        }
        catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public void loadAssets(String folder) {
        List<String> valid = Arrays.asList(ITextures.fileNames);

        try {
            for (String asset : Game.activity.getAssets().list("gfx")) {
                final String name = asset.substring(0, asset.length() - 4);

                if (valid.contains(name)) {
                    InputStream stream = tryGetAsset(asset);

                    if (stream == null) {
                        Log.e("BitmapManager", "Failed to load asset: " + name);
                        continue;
                    }
                    bitmaps.put(name, BitmapFactory.decodeStream(stream));
                }
            }
        } catch (IOException exception) {
            Log.e("BitmapManager", "Failed to load game assets! \n");
            exception.printStackTrace();
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void put(String key, Bitmap bitmap) {
        this.bitmaps.put(key, bitmap);
    }

    public Bitmap get(String name) {
        Bitmap bitmap = bitmaps.get(name);
        if (bitmap == null) {
            Log.e("BitmapManager", "Bitmap \"" + name + "\" does not exist or isn't loaded!");
        }
        return bitmap;
    }

    public boolean contains(String name) {
        return this.bitmaps.containsKey(name);
    }
}
