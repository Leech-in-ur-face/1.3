package com.reco1l.interfaces;

// Created by Reco1l on 18/9/22 20:50

import com.reco1l.enums.Screens;

import org.anddev.andengine.entity.scene.Scene;

public interface SceneHandler {

    Screens getIdentifier();

    default void onPause() {}

    default void onResume() {}

    default void onSceneChange(Scene oldScene, Scene newScene) {}

    default boolean onBackPress() {
        return false;
    }
}
