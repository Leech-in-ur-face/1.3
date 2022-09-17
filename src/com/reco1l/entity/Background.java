package com.reco1l.entity;

// Created by Reco1l on 26/6/22 18:22

import com.reco1l.utils.listeners.ModifierListener;
import com.reco1l.interfaces.IMainClasses;
import com.reco1l.ui.platform.UI;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseQuadInOut;
import org.anddev.andengine.util.modifier.ease.IEaseFunction;

import ru.nsu.ccfit.zuev.osu.Config;

public class Background implements IMainClasses {

    private Scene parent;

    public Rectangle dim;
    private TextureRegion defaultTexture;
    private Sprite background, lastBackground;

    private final int resW = Config.getRES_WIDTH();
    private final int resH = Config.getRES_HEIGHT();

    private final IEaseFunction interpolator = EaseQuadInOut.getInstance();

    //--------------------------------------------------------------------------------------------//

    private float height(TextureRegion texture) {
        if (texture == null)
            return 0;
        return texture.getHeight() * (resW / (float) texture.getWidth());
    }

    public void draw(Scene scene) {
        parent = scene;
        defaultTexture = resources.getTexture("menu-background");

        parent.setBackground(new ColorBackground(0, 0, 0));
        lastBackground = new Sprite(0, 0, resW, resH, defaultTexture);
        parent.attachChild(lastBackground, 0);

        dim = new Rectangle(0, 0, resW, resH);
        dim.setColor(0, 0, 0);
        dim.setAlpha(0);
        parent.attachChild(dim, 1);
    }

    public void redraw(String path) {
        TextureRegion texture;

        if (!Config.isSafeBeatmapBg() && path != null) {
            texture = resources.loadBackground(path);
        } else {
            texture = defaultTexture;
        }

        dim.setAlpha(UI.mainMenu.isMenuShowing ? 0.3f : 0);

        background = new Sprite(0, (resH - height(texture)) / 2f, resW, height(texture), texture);
        background.setScale(UI.mainMenu.isMenuShowing ? 1.2f : 1f);

        ModifierListener listener = new ModifierListener() {
            @Override
            public void onModifierStarted(IModifier<IEntity> modifier, IEntity item) {
                parent.attachChild(background, 0);
                global.getMainScene().spectrum.updateColor(path);
            }

            @Override
            public void onModifierFinished(IModifier<IEntity> modifier, IEntity item) {
                mActivity.runOnUpdateThread(item::detachSelf);
            }
        };

        lastBackground.registerEntityModifier(new AlphaModifier(1.5f, 1, 0, listener));
        lastBackground = background;
    }

    public void zoomIn() {
        if (background == null || UI.mainMenu.isMenuShowing)
            return;
        background.clearEntityModifiers();
        dim.clearEntityModifiers();

        dim.registerEntityModifier(new AlphaModifier(0.4f, 0, 0.3f, interpolator));
        background.registerEntityModifier(new ScaleModifier(0.4f, 1, 1.2f, interpolator));
    }

    public void zoomOut(boolean isTransition) {
        if (background == null || !UI.mainMenu.isMenuShowing)
            return;

        background.clearEntityModifiers();
        background.registerEntityModifier(new ScaleModifier(0.4f, 1.2f, 1, interpolator));

        if (isTransition)
            return;

        dim.clearEntityModifiers();
        dim.registerEntityModifier(new AlphaModifier(0.4f, 0.3f, 0, interpolator));
    }

}
