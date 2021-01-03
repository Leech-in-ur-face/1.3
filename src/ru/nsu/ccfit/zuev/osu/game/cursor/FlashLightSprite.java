package ru.nsu.ccfit.zuev.osu.game.cursor;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.ResourceManager;

public class FlashLightSprite extends Entity{
    private Sprite sprite;
    private boolean showing = false;
    private float size = 8f;
    private float holdX = 0f;
    private float holdY = 0f;
    private float leadScale = 0f;

    public FlashLightSprite() {
        TextureRegion tex = ResourceManager.getInstance().getTexture("flashlight_cursor");
        sprite = new Sprite(-tex.getWidth() / 2, -tex.getHeight() / 2, tex);
        sprite.setScale(size);
        attachChild(sprite);
    }

    public void setShowing(boolean showing) {
        this.showing = showing;
    }

    public void setSliderHoldPos(float xPos, float yPos) {
        this.holdX = xPos;
        this.holdY = yPos;
    }

    public void update(float pSecondsElapsed, int combo) {
        if (showing){
            if (!sprite.isVisible())
                sprite.setVisible(true);
            if (sprite.getAlpha() < 1)
                sprite.setAlpha(1);
        } else {
            if (sprite.getAlpha() > 0)
                sprite.setAlpha(Math.max(0, sprite.getAlpha() - 4f * pSecondsElapsed));
            else
                sprite.setVisible(false);
        }
        if (combo > 200) {
            sprite.setScale(0.6f * size);
            leadScale = 0.6f;
        } else if (combo > 100) {
            sprite.setScale(0.8f * size);
            leadScale = 0.8f;
        } else {
            sprite.setScale(1f * size);
            leadScale = 1.0f;
        }
        if (holdX != 0 && holdY != 0) {
            if (leadScale == 1.0f) {
                sprite.setScale(0.8f * size);
            } else if (leadScale == 0.8f) {
                sprite.setScale(0.6f * size);
            } else {
                sprite.setScale(0.4f * size);
            }
        }
    }
}