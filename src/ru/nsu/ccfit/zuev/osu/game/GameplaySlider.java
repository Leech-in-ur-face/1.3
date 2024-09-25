package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.LinePath;
import com.edlplan.osu.support.slider.SliderBody;
import com.reco1l.osu.Execution;
import com.reco1l.osu.graphics.AnimatedSprite;
import com.reco1l.osu.graphics.ExtendedSprite;
import com.reco1l.osu.graphics.Modifiers;
import com.reco1l.osu.graphics.Origin;
import com.reco1l.osu.playfield.CirclePiece;
import com.reco1l.osu.playfield.NumberedCirclePiece;
import com.reco1l.osu.playfield.SliderTickContainer;
import com.rian.osu.beatmap.hitobject.Slider;
import com.rian.osu.beatmap.sections.BeatmapControlPoints;
import com.rian.osu.math.Interpolation;
import com.rian.osu.mods.ModHidden;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.util.MathUtils;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseQuadOut;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.game.GameHelper.SliderPath;
import ru.nsu.ccfit.zuev.osu.helper.DifficultyHelper;
import ru.nsu.ccfit.zuev.osu.helper.ModifierListener;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

import java.util.BitSet;

public class GameplaySlider extends GameObject {

    private final ExtendedSprite approachCircle;
    private final ExtendedSprite startArrow, endArrow;
    private Slider beatmapSlider;
    private final PointF curveEndPos = new PointF();
    private Scene scene;
    private GameObjectListener listener;
    private SliderPath path;
    private double passedTime;
    private float timePreempt;
    private int completedSpanCount;
    private boolean reverse;
    private boolean slidingSamplesPlaying;

    private int currentNestedObjectIndex;
    private int ticksGot;
    private double tickTime;
    private double tickInterval;
    private int currentTickSpriteIndex;

    private PointF ballPos;
    private final ExtendedSprite followCircle;

    // Temporarily used PointF to avoid allocations
    private final PointF tmpPoint = new PointF();
    private float ballAngle;

    private boolean kiai;
    private final RGBColor bodyColor = new RGBColor();
    private final RGBColor circleColor = new RGBColor();

    //for replay
    private int firstHitAccuracy;
    private final BitSet tickSet = new BitSet();
    private int replayTickIndex;

    private LinePath superPath = null;
    private boolean preStageFinish = false;

    private final SliderBody sliderBody;


    /**
     * The slider ball sprite.
     */
    private final AnimatedSprite ball;

    /**
     * The start circle piece of the slider.
     */
    private final NumberedCirclePiece headCirclePiece;

    /**
     * The end circle piece of the slider.
     */
    private final CirclePiece tailCirclePiece;

    /**
     * The slider tick container.
     */
    private final SliderTickContainer tickContainer;

    /**
     * Whether the slider has ended (and all its spans).
     */
    private boolean isOver;

    /**
     * Whether the follow circle sprite is being animated.
     */
    private boolean isFollowCircleAnimating;

    /**
     * Whether the cursor is in the slider's radius.
     */
    private boolean isInRadius;


    public GameplaySlider() {

        headCirclePiece = new NumberedCirclePiece("sliderstartcircle", "sliderstartcircleoverlay");
        tailCirclePiece = new CirclePiece("sliderendcircle", "sliderendcircleoverlay");

        approachCircle = new ExtendedSprite();
        approachCircle.setOrigin(Origin.Center);
        approachCircle.setTextureRegion(ResourceManager.getInstance().getTexture("approachcircle"));

        startArrow = new ExtendedSprite();
        startArrow.setOrigin(Origin.Center);
        startArrow.setTextureRegion(ResourceManager.getInstance().getTexture("reversearrow"));

        endArrow = new ExtendedSprite();
        endArrow.setOrigin(Origin.Center);
        endArrow.setTextureRegion(ResourceManager.getInstance().getTexture("reversearrow"));

        ball = new AnimatedSprite("sliderb", false);

        // Avoid to use AnimatedSprite if not necessary.
        if (ResourceManager.getInstance().isTextureLoaded("sliderfollowcircle-0")) {
            followCircle = new AnimatedSprite("sliderfollowcircle", true, OsuSkin.get().getAnimationFramerate());
        } else {
            followCircle = new ExtendedSprite();
            followCircle.setTextureRegion(ResourceManager.getInstance().getTexture("sliderfollowcircle"));
        }

        sliderBody = new SliderBody(OsuSkin.get().isSliderHintEnable());
        tickContainer = new SliderTickContainer();
    }

    public void init(final GameObjectListener listener, final Scene scene,
                     final Slider beatmapSlider, final float secPassed, final RGBColor comboColor,
                     final RGBColor borderColor, final float tickRate, final BeatmapControlPoints controlPoints,
                     final SliderPath sliderPath) {
        this.listener = listener;
        this.scene = scene;
        this.beatmapSlider = beatmapSlider;

        var stackedPosition = beatmapSlider.getGameplayStackedPosition();
        position.set(stackedPosition.x, stackedPosition.y);

        endsCombo = beatmapSlider.isLastInCombo();
        passedTime = secPassed - (float) beatmapSlider.startTime / 1000;
        slidingSamplesPlaying = false;
        path = sliderPath;

        float scale = beatmapSlider.getGameplayScale();

        isOver = false;
        isFollowCircleAnimating = false;
        isInRadius = false;

        reverse = false;
        startHit = false;
        ticksGot = 0;
        tickTime = 0;
        completedSpanCount = 0;
        currentTickSpriteIndex = 0;
        replayTickIndex = 0;
        firstHitAccuracy = 0;
        tickSet.clear();
        kiai = GameHelper.isKiai();
        preStageFinish = false;
        bodyColor.set(comboColor.r(), comboColor.g(), comboColor.b());
        if (!OsuSkin.get().isSliderFollowComboColor()) {
            var sliderBodyColor = OsuSkin.get().getSliderBodyColor();
            bodyColor.set(sliderBodyColor.r(), sliderBodyColor.g(), sliderBodyColor.b());
        }
        circleColor.set(comboColor.r(), comboColor.g(), comboColor.b());
        currentNestedObjectIndex = 0;

        // Start circle piece
        headCirclePiece.setScale(scale);
        headCirclePiece.setCircleColor(comboColor.r(), comboColor.g(), comboColor.b());
        headCirclePiece.setAlpha(0);
        headCirclePiece.setPosition(this.position.x, this.position.y);
        int comboNum = beatmapSlider.getIndexInCurrentCombo() + 1;
        if (OsuSkin.get().isLimitComboTextLength()) {
            comboNum %= 10;
        }
        headCirclePiece.setNumberText(comboNum);
        headCirclePiece.setNumberScale(OsuSkin.get().getComboTextScale());

        approachCircle.setColor(comboColor.r(), comboColor.g(), comboColor.b());
        approachCircle.setScale(scale * 3);
        approachCircle.setAlpha(0);
        approachCircle.setPosition(this.position.x, this.position.y);

        if (GameHelper.isHidden()) {
            approachCircle.setVisible(Config.isShowFirstApproachCircle() && beatmapSlider.isFirstNote());
        }

        // End circle
        curveEndPos.x = path.getX(path.pointCount - 1);
        curveEndPos.y = path.getY(path.pointCount - 1);

        tailCirclePiece.setScale(scale);
        tailCirclePiece.setCircleColor(comboColor.r(), comboColor.g(), comboColor.b());
        tailCirclePiece.setAlpha(0);

        if (Config.isSnakingInSliders()) {
            tailCirclePiece.setPosition(this.position.x, this.position.y);
        } else {
            tailCirclePiece.setPosition(curveEndPos.x, curveEndPos.y);
        }

        // Repeat arrow at start
        int spanCount = beatmapSlider.getSpanCount();
        if (spanCount > 2) {
            startArrow.setAlpha(0);
            startArrow.setScale(scale);
            startArrow.setRotation(MathUtils.radToDeg(Utils.direction(this.position.x, this.position.y, path.getX(1), path.getY(1))));
            startArrow.setPosition(this.position.x, this.position.y);

            scene.attachChild(startArrow, 0);
        }

        timePreempt = (float) beatmapSlider.timePreempt / 1000;
        float fadeInDuration = (float) beatmapSlider.timeFadeIn / 1000;

        if (GameHelper.isHidden()) {
            float fadeOutDuration = timePreempt * (float) ModHidden.FADE_OUT_DURATION_MULTIPLIER;

            headCirclePiece.registerEntityModifier(Modifiers.sequence(
                Modifiers.fadeIn(fadeInDuration),
                Modifiers.fadeOut(fadeOutDuration)
            ));

            tailCirclePiece.registerEntityModifier(Modifiers.sequence(
                Modifiers.fadeIn(fadeInDuration),
                Modifiers.fadeOut(fadeOutDuration)
            ));

        } else {
            headCirclePiece.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
            tailCirclePiece.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
        }

        if (approachCircle.isVisible()) {
            approachCircle.registerEntityModifier(Modifiers.alpha(Math.min(fadeInDuration * 2, timePreempt), 0, 0.9f));
            approachCircle.registerEntityModifier(Modifiers.scale(timePreempt, scale * 3, scale));
        }

        scene.attachChild(headCirclePiece, 0);
        scene.attachChild(approachCircle);
        // Repeat arrow at end
        if (spanCount > 1) {
            endArrow.setAlpha(0);
            endArrow.setScale(scale);
            endArrow.setRotation(MathUtils.radToDeg(Utils.direction(curveEndPos.x, curveEndPos.y, path.getX(path.pointCount - 2), path.getY(path.pointCount - 2))));

            if (Config.isSnakingInSliders()) {
                endArrow.setPosition(this.position.x, this.position.y);
            } else {
                endArrow.setPosition(curveEndPos.x, curveEndPos.y);
            }

            scene.attachChild(endArrow, 0);
        }
        scene.attachChild(tailCirclePiece, 0);

        var timingControlPoint = controlPoints.timing.controlPointAt(beatmapSlider.startTime);
        tickInterval = timingControlPoint.msPerBeat / 1000 / tickRate;

        tickContainer.init(beatmapSlider);
        scene.attachChild(tickContainer, 0);

        // Slider track
        if (path.pointCount != 0) {
            superPath = new LinePath();

            for (int i = 0; i < path.pointCount; ++i) {

                var x = path.getX(i);
                var y = path.getY(i);

                superPath.add(new Vec2(x, y));
            }
            superPath.measure();
            superPath.bufferLength(path.getLength(path.lengthCount - 1));
            superPath = superPath.fitToLinePath();
            superPath.measure();

            sliderBody.setPath(superPath, Config.isSnakingInSliders());
            sliderBody.setBackgroundWidth(OsuSkin.get().getSliderBodyWidth() * scale);
            sliderBody.setBackgroundColor(bodyColor.r(), bodyColor.g(), bodyColor.b(), OsuSkin.get().getSliderBodyBaseAlpha());

            sliderBody.setBorderWidth(OsuSkin.get().getSliderBorderWidth() * scale);
            sliderBody.setBorderColor(borderColor.r(), borderColor.g(), borderColor.b());

            if (OsuSkin.get().isSliderHintEnable() && beatmapSlider.getDistance() > OsuSkin.get().getSliderHintShowMinLength()) {
                sliderBody.setHintVisible(true);
                sliderBody.setHintWidth(OsuSkin.get().getSliderHintWidth() * scale);

                RGBColor hintColor = OsuSkin.get().getSliderHintColor();
                if (hintColor != null) {
                    sliderBody.setHintColor(hintColor.r(), hintColor.g(), hintColor.b(), OsuSkin.get().getSliderHintAlpha());
                } else {
                    sliderBody.setHintColor(bodyColor.r(), bodyColor.g(), bodyColor.b(), OsuSkin.get().getSliderHintAlpha());
                }
            } else {
                sliderBody.setHintVisible(false);
            }

            scene.attachChild(sliderBody, 0);
        }

        if (Config.isDimHitObjects()) {

            // Source: https://github.com/peppy/osu/blob/60271fb0f7e091afb754455f93180094c63fc3fb/osu.Game.Rulesets.Osu/Objects/Drawables/DrawableOsuHitObject.cs#L101
            var dimDelaySec = timePreempt - objectHittableRange;
            var colorDim = 195f / 255f;

            headCirclePiece.setColor(colorDim, colorDim, colorDim);
            headCirclePiece.registerEntityModifier(Modifiers.sequence(
                Modifiers.delay(dimDelaySec),
                Modifiers.color(0.1f,
                    headCirclePiece.getRed(), 1f,
                    headCirclePiece.getGreen(), 1f,
                    headCirclePiece.getBlue(), 1f
                )
            ));

            tailCirclePiece.setColor(colorDim, colorDim, colorDim);
            tailCirclePiece.registerEntityModifier(Modifiers.sequence(
                Modifiers.delay(dimDelaySec),
                Modifiers.color(0.1f,
                    tailCirclePiece.getRed(), 1f,
                    tailCirclePiece.getGreen(), 1f,
                    tailCirclePiece.getBlue(), 1f
                )
            ));

            endArrow.setColor(colorDim, colorDim, colorDim);
            endArrow.registerEntityModifier(Modifiers.sequence(
                Modifiers.delay(dimDelaySec),
                Modifiers.color(0.1f,
                    endArrow.getRed(), 1f,
                    endArrow.getGreen(), 1f,
                    endArrow.getBlue(), 1f
                )
            ));

            sliderBody.setColor(colorDim, colorDim, colorDim);
            sliderBody.registerEntityModifier(Modifiers.sequence(
                Modifiers.delay(dimDelaySec),
                Modifiers.color(0.1f,
                    sliderBody.getRed(), 1f,
                    sliderBody.getGreen(), 1f,
                    sliderBody.getBlue(), 1f
                )
            ));

            tickContainer.setColor(colorDim, colorDim, colorDim);
            tickContainer.registerEntityModifier(Modifiers.sequence(
                Modifiers.delay(dimDelaySec),
                Modifiers.color(0.1f,
                    tickContainer.getRed(), 1f,
                    tickContainer.getGreen(), 1f,
                    tickContainer.getBlue(), 1f
                )
            ));
        }

        applyBodyFadeAdjustments(fadeInDuration);
    }

    private PointF getPositionAt(final float percentage, final boolean updateBallAngle, final boolean updateEndArrowRotation) {
        if (path.pointCount == 0) {
            tmpPoint.set(position);
            return tmpPoint;
        }

        if (percentage >= 1) {
            tmpPoint.set(curveEndPos);
            return tmpPoint;
        } else if (percentage <= 0) {
            if (path.pointCount >= 2) {
                if (updateBallAngle) {
                    ballAngle = MathUtils.radToDeg(Utils.direction(path.getX(1), path.getY(1), position.x, position.y));
                }

                if (updateEndArrowRotation) {
                    endArrow.setRotation(MathUtils.radToDeg(Utils.direction(position.x, position.y, path.getX(1), path.getY(1))));
                }
            }

            tmpPoint.set(position);
            return tmpPoint;
        }

        // Directly taken from library-owned SliderPath
        int left = 0;
        int right = path.lengthCount - 2;
        float currentLength = percentage * path.getLength(path.lengthCount - 1);

        while (left <= right) {
            int pivot = left + ((right - left) >> 1);
            float length = path.getLength(pivot);

            if (length < currentLength) {
                left = pivot + 1;
            } else if (length > currentLength) {
                right = pivot - 1;
            } else {
                break;
            }
        }

        int index = left - 1;
        float lengthProgress = (currentLength - path.getLength(index)) / (path.getLength(index + 1) - path.getLength(index));

        var currentPointX = path.getX(index);
        var currentPointY = path.getY(index);

        var nextPointX = path.getX(index + 1);
        var nextPointY = path.getY(index + 1);

        var p = tmpPoint;

        p.set(
            Interpolation.linear(currentPointX, nextPointX, lengthProgress),
            Interpolation.linear(currentPointY, nextPointY, lengthProgress)
        );

        if (updateBallAngle) {
            ballAngle = MathUtils.radToDeg(Utils.direction(currentPointX, currentPointY, nextPointX, nextPointY));
        }

        if (updateEndArrowRotation) {
            endArrow.setRotation(MathUtils.radToDeg(Utils.direction(nextPointX, nextPointY, currentPointX, currentPointY)));
        }

        return p;
    }

    private void removeFromScene() {
        if (scene == null) {
            return;
        }

        if (GameHelper.isHidden()) {
            sliderBody.detachSelf();

            // If the animation is enabled, at this point it will be still animating.
            if (!Config.isAnimateFollowCircle() || !isFollowCircleAnimating) {
                poolObject();
            }
        } else {
            sliderBody.registerEntityModifier(Modifiers.fadeOut(0.24f, new ModifierListener() {

                @Override
                public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                    Execution.updateThread(() -> {
                        sliderBody.detachSelf();

                        // We can pool the hit object once all animations are finished.
                        // The slider body is the last object to finish animating.
                        poolObject();
                    });
                }
            }));
        }

        ball.registerEntityModifier(Modifiers.fadeOut(0.1f, new ModifierListener() {
            @Override
            public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                Execution.updateThread(ball::detachSelf);
            }
        }));

        // Follow circle might still be animating when the slider is removed from the scene.
        if (!Config.isAnimateFollowCircle() || !isFollowCircleAnimating) {
            followCircle.detachSelf();
        }

        headCirclePiece.detachSelf();
        tailCirclePiece.detachSelf();
        approachCircle.detachSelf();
        startArrow.detachSelf();
        endArrow.detachSelf();
        tickContainer.detachSelf();

        listener.removeObject(this);
        stopSlidingSamples();
        scene = null;
    }

    public void poolObject() {

        headCirclePiece.clearEntityModifiers();
        tailCirclePiece.clearEntityModifiers();

        startArrow.clearEntityModifiers();
        endArrow.clearEntityModifiers();
        approachCircle.clearEntityModifiers();
        followCircle.clearEntityModifiers();
        ball.clearEntityModifiers();
        sliderBody.clearEntityModifiers();
        tickContainer.clearEntityModifiers();

        GameHelper.putPath(path);
        GameObjectPool.getInstance().putSlider(this);
    }

    private void onSpanFinish() {
        ++completedSpanCount;

        int totalSpanCount = beatmapSlider.getSpanCount();
        int remainingSpans = totalSpanCount - completedSpanCount;
        boolean stillHasSpan = remainingSpans > 0;

        if (isInRadius && replayObjectData == null ||
                replayObjectData != null && replayObjectData.tickSet.get(replayTickIndex)) {
            playCurrentNestedObjectHitSound();
            ticksGot++;
            tickSet.set(replayTickIndex++, true);

            if (stillHasSpan) {
                listener.onSliderHit(id, 30, null,
                        reverse ? position : curveEndPos,
                        false, bodyColor, GameObjectListener.SLIDER_REPEAT);
            }
        } else {
            tickSet.set(replayTickIndex++, false);

            if (stillHasSpan) {
                listener.onSliderHit(id, -1, null,
                        reverse ? position : curveEndPos,
                        false, bodyColor, GameObjectListener.SLIDER_REPEAT);
            }
        }

        currentNestedObjectIndex++;

        // If slider has more spans
        if (stillHasSpan) {
            double spanDuration = beatmapSlider.getSpanDuration() / 1000;
            reverse = !reverse;
            passedTime -= spanDuration;
            tickTime = passedTime;

            if (reverse) {
                // In reversed spans, a slider's tick position remains the same as the non-reversed span.
                // Therefore, we need to offset the tick time such that the travelled time is (tickInterval - nextTickTime).
                tickTime += tickInterval - spanDuration % tickInterval;
            }

            ball.setFlippedHorizontal(reverse);
            // Restore ticks
            for (int i = 0, size = tickContainer.getChildCount(); i < size; i++) {
                tickContainer.getChild(i).setAlpha(1f);
            }

            currentTickSpriteIndex = reverse ? tickContainer.getChildCount() - 1 : 0;

            // Setting visibility of repeat arrows
            if (reverse) {
                if (remainingSpans <= 2) {
                    endArrow.setAlpha(0);
                }

                if (remainingSpans > 1) {
                    startArrow.setAlpha(1);
                }
            } else if (remainingSpans <= 2) {
                startArrow.setAlpha(0);
            }

            ((GameScene) listener).onSliderReverse(
                    !reverse ? position : curveEndPos,
                    reverse ? endArrow.getRotation() : startArrow.getRotation(),
                    bodyColor);

            if (passedTime >= spanDuration) {
                // This condition can happen under low frame rate and/or short span duration, which will cause all
                // slider tick judgements in this span to be skipped. Ensure that all slider ticks in the current
                // span has been judged before proceeding to the next span.
                judgeSliderTicks();

                onSpanFinish();
            }

            return;
        }
        isOver = true;

        // Calculating score
        int firstHitScore = 0;
        if (GameHelper.isScoreV2()) {
            // If ScoreV2 is active, the accuracy of hitting the slider head is additionally accounted for when judging the entire slider:
            // Getting a 300 for a slider requires getting a 300 judgement for the slider head.
            // Getting a 100 for a slider requires getting a 100 judgement or better for the slider head.
            DifficultyHelper diffHelper = GameHelper.getDifficultyHelper();
            float od = GameHelper.getOverallDifficulty();

            if (Math.abs(firstHitAccuracy) <= diffHelper.hitWindowFor300(od) * 1000) {
                firstHitScore = 300;
            } else if (Math.abs(firstHitAccuracy) <= diffHelper.hitWindowFor100(od) * 1000) {
                firstHitScore = 100;
            }
        }
        int score = 0;
        if (ticksGot > 0) {
            score = 50;
        }
        int totalTicks = beatmapSlider.getNestedHitObjects().size();
        if (ticksGot >= totalTicks / 2 && (!GameHelper.isScoreV2() || firstHitScore >= 100)) {
            score = 100;
        }
        if (ticksGot >= totalTicks && (!GameHelper.isScoreV2() || firstHitScore == 300)) {
            score = 300;
        }
        // If slider was in reverse mode, we should swap start and end points
        if (reverse) {
            GameplaySlider.this.listener.onSliderHit(id, score,
                    curveEndPos, position, endsCombo, bodyColor, GameObjectListener.SLIDER_END);
        } else {
            GameplaySlider.this.listener.onSliderHit(id, score, position,
                    curveEndPos, endsCombo, bodyColor, GameObjectListener.SLIDER_END);
        }
        if (!startHit) {
            firstHitAccuracy = (int) (GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getOverallDifficulty()) * 1000 + 13);
        }
        listener.onSliderEnd(id, firstHitAccuracy, tickSet);
        // Remove slider from scene

        if (Config.isAnimateFollowCircle() && isInRadius) {
            isFollowCircleAnimating = true;

            followCircle.clearEntityModifiers();
            followCircle.registerEntityModifier(Modifiers.scale(0.2f, followCircle.getScaleX(), followCircle.getScaleX() * 0.8f, null, EaseQuadOut.getInstance()));
            followCircle.registerEntityModifier(Modifiers.alpha(0.2f, followCircle.getAlpha(), 0f, new ModifierListener() {
                @Override
                public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                    Execution.updateThread(() -> {
                        followCircle.detachSelf();

                        // When hidden mod is enabled, the follow circle is the last object to finish animating.
                        if (GameHelper.isHidden()) {
                            poolObject();
                        }
                    });
                    isFollowCircleAnimating = false;
                }
            }));
        }

        removeFromScene();
    }

    private boolean canBeHit() {
        return passedTime >= -objectHittableRange;
    }

    private boolean isHit() {
        float radius = Utils.sqr((float) beatmapSlider.getGameplayRadius());
        for (int i = 0, count = listener.getCursorsCount(); i < count; i++) {

            var inPosition = Utils.squaredDistance(position, listener.getMousePos(i)) <= radius;
            if (GameHelper.isRelaxMod() && passedTime >= 0 && inPosition) {
                return true;
            }

            var isPressed = listener.isMousePressed(this, i);
            if (isPressed && inPosition) {
                return true;
            } else if (GameHelper.isAutopilotMod() && isPressed) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void update(final float dt) {

        if (scene == null) {
            return;
        }
        passedTime += dt;

        if (!startHit) // If we didn't get start hit(click)
        {
            // If it's too late, mark this hit missing
            if (passedTime > GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getOverallDifficulty())) {
                startHit = true;
                currentNestedObjectIndex++;
                listener.onSliderHit(id, -1, null, position, false, bodyColor, GameObjectListener.SLIDER_START);
                firstHitAccuracy = (int) (passedTime * 1000);
            } else if (autoPlay && passedTime >= 0) {
                startHit = true;
                playCurrentNestedObjectHitSound();
                currentNestedObjectIndex++;
                ticksGot++;
                listener.onSliderHit(id, 30, null, position, false, bodyColor, GameObjectListener.SLIDER_START);
            } else if (replayObjectData != null &&
                    Math.abs(replayObjectData.accuracy / 1000f) <= GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getOverallDifficulty()) &&
                    passedTime + dt / 2 > replayObjectData.accuracy / 1000f) {
                startHit = true;
                playCurrentNestedObjectHitSound();
                currentNestedObjectIndex++;
                ticksGot++;
                listener.onSliderHit(id, 30, null, position, false, bodyColor, GameObjectListener.SLIDER_START);
            } else if (isHit() && canBeHit()) {
                listener.registerAccuracy(passedTime);
                startHit = true;
                ticksGot++;
                firstHitAccuracy = (int) (passedTime * 1000);

                if (-passedTime < GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getOverallDifficulty())) {
                    playCurrentNestedObjectHitSound();
                    listener.onSliderHit(id, 30, null, position,
                            false, bodyColor, GameObjectListener.SLIDER_START);
                } else {
                    listener.onSliderHit(id, -1, null, position,
                            false, bodyColor, GameObjectListener.SLIDER_START);
                }

                currentNestedObjectIndex++;
            }
        }

        if (GameHelper.isKiai()) {
            var kiaiModifier = (float) Math.max(0, 1 - GameHelper.getCurrentBeatTime() / GameHelper.getBeatLength()) * 0.5f;
            var r = Math.min(1, circleColor.r() + (1 - circleColor.r()) * kiaiModifier);
            var g = Math.min(1, circleColor.g() + (1 - circleColor.g()) * kiaiModifier);
            var b = Math.min(1, circleColor.b() + (1 - circleColor.b()) * kiaiModifier);
            kiai = true;
            headCirclePiece.setCircleColor(r, g, b);
        } else if (kiai) {
            headCirclePiece.setCircleColor(circleColor.r(), circleColor.g(), circleColor.b());
            kiai = false;
        }

        if (passedTime < 0) // we at approach time
        {
            if (startHit) {
                // Hide the approach circle if the slider is already hit.
                approachCircle.clearEntityModifiers();
                approachCircle.setAlpha(0);
            }

            float percentage = (float) (1 + passedTime / timePreempt);

            if (percentage <= 0.5f) {
                // Following core doing a very cute show animation ^_^"
                percentage = Math.min(1, percentage * 2);

                for (int i = 0, size = tickContainer.getChildCount(); i < size; i++) {
                    if (percentage > (float) (i + 1) / size) {
                        tickContainer.getChild(i).setAlpha(1f);
                    }
                }

                if (beatmapSlider.getSpanCount() > 1) {
                    endArrow.setAlpha(percentage);
                }

                if (Config.isSnakingInSliders()) {
                    if (superPath != null && sliderBody != null) {
                        float l = superPath.getMeasurer().maxLength() * percentage;

                        sliderBody.setEndLength(l);
                    }

                    var position = getPositionAt(percentage, false, true);

                    tailCirclePiece.setPosition(position.x, position.y);
                    endArrow.setPosition(position.x, position.y);
                }
            } else if (percentage - dt / timePreempt <= 0.5f) {

                for (int i = 0, size = tickContainer.getChildCount(); i < size; i++) {
                    tickContainer.getChild(i).setAlpha(1f);
                }

                if (beatmapSlider.getSpanCount() > 1) {
                    endArrow.setAlpha(1);
                }
                if (Config.isSnakingInSliders()) {
                    if (!preStageFinish && superPath != null && sliderBody != null) {
                        sliderBody.setEndLength(superPath.getMeasurer().maxLength());
                        preStageFinish = true;
                    }

                    endArrow.setRotation(
                        MathUtils.radToDeg(Utils.direction(curveEndPos.x, curveEndPos.y, path.getX(path.pointCount - 2), path.getY(path.pointCount - 2)))
                    );

                    tailCirclePiece.setPosition(curveEndPos.x, curveEndPos.y);
                    endArrow.setPosition(curveEndPos.x, curveEndPos.y);
                }
            }
            return;
        }

        headCirclePiece.setAlpha(0f);

        float scale = beatmapSlider.getGameplayScale();

        if (!ball.hasParent()) {
            approachCircle.clearEntityModifiers();
            approachCircle.setAlpha(0);

            ball.setFps((float) beatmapSlider.getVelocity() * 100 * scale);
            ball.setScale(scale);
            ball.setFlippedHorizontal(false);
            ball.registerEntityModifier(Modifiers.fadeIn(0.1f));

            followCircle.setAlpha(0);
            if (!Config.isAnimateFollowCircle()) {
                followCircle.setScale(scale);
            }

            scene.attachChild(ball);
            scene.attachChild(followCircle);
        }

        // Ball position
        final float spanDuration = (float) beatmapSlider.getSpanDuration() / 1000;
        final float percentage = (float) passedTime / spanDuration;
        ballPos = getPositionAt(reverse ? 1 - percentage : percentage, true, false);

        // Calculating if cursor in follow circle bounds
        float trackingDistanceThresholdSquared = getTrackingDistanceThresholdSquared();
        boolean inRadius = false;

        for (int i = 0, cursorCount = listener.getCursorsCount(); i < cursorCount; i++) {
            var isPressed = listener.isMouseDown(i);

            if (GameHelper.isAutopilotMod() && isPressed) {
                inRadius = true;
                break;
            }

            if (autoPlay || (isPressed &&
                    Utils.squaredDistance(listener.getMousePos(i), ballPos) <= trackingDistanceThresholdSquared)) {
                inRadius = true;
                break;
            }
        }

        listener.onTrackingSliders(inRadius);
        tickTime += dt;

        if (Config.isAnimateFollowCircle()) {
            float realSliderDuration = (float) beatmapSlider.getDuration() / 1000f;
            float remainTime = realSliderDuration - (float) passedTime;

            if (inRadius && !isInRadius) {
                isInRadius = true;
                isFollowCircleAnimating = true;
                playSlidingSamples();

                // If alpha doesn't equal 0 means that it has been into an animation before
                float initialScale = followCircle.getAlpha() == 0 ? scale * 0.5f : followCircle.getScaleX();

                followCircle.clearEntityModifiers();
                followCircle.registerEntityModifier(Modifiers.alpha(Math.min(remainTime, 0.06f), followCircle.getAlpha(), 1f));
                followCircle.registerEntityModifier(Modifiers.scale(Math.min(remainTime, 0.18f), initialScale, scale, new ModifierListener() {
                    @Override
                    public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                        isFollowCircleAnimating = false;
                    }
                }, EaseQuadOut.getInstance()));
            } else if (!inRadius && isInRadius) {
                isInRadius = false;
                isFollowCircleAnimating = true;
                stopSlidingSamples();

                followCircle.clearEntityModifiers();
                followCircle.registerEntityModifier(Modifiers.scale(0.1f, followCircle.getScaleX(), scale * 2f));
                followCircle.registerEntityModifier(Modifiers.alpha(0.1f, followCircle.getAlpha(), 0f, new ModifierListener() {
                    @Override
                    public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                        if (isOver) {
                            Execution.updateThread(pItem::detachSelf);
                        }
                        isFollowCircleAnimating = false;
                    }
                }));
            }
        } else {
            if (inRadius && !isInRadius) {
                playSlidingSamples();
            } else if (!inRadius && isInRadius) {
                stopSlidingSamples();
            }

            isInRadius = inRadius;
            followCircle.setAlpha(inRadius ? 1 : 0);
        }

        judgeSliderTicks();

        // Setting position of ball and follow circle
        followCircle.setPosition(ballPos.x - followCircle.getWidth() / 2,
                ballPos.y - followCircle.getHeight() / 2);
        ball.setPosition(ballPos.x - ball.getWidth() / 2,
                ballPos.y - ball.getHeight() / 2);
        ball.setRotation(ballAngle);

        if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) {
            listener.updateAutoBasedPos(ballPos.x, ballPos.y);
        }

        // If we got 100% time, finishing slider
        if (percentage >= 1) {
            onSpanFinish();
        }
    }

    private float getTrackingDistanceThresholdSquared() {
        float radius = (float) beatmapSlider.getGameplayRadius();
        float distanceThresholdSquared = radius * radius;

        if (isInRadius) {
            // Multiply by 4 as the follow circle radius is 2 times larger than the object radius.
            distanceThresholdSquared *= 4;
        }

        return distanceThresholdSquared;
    }

    private void judgeSliderTicks() {
        if (tickContainer.getChildCount() == 0) {
            return;
        }

        float scale = beatmapSlider.getGameplayScale();

        while (tickTime >= tickInterval) {
            tickTime -= tickInterval;
            var tickSprite = tickContainer.getChild(currentTickSpriteIndex);

            if (tickSprite.getAlpha() == 0) {
                // All ticks in the current span had been judged.
                break;
            }

            if (isInRadius && replayObjectData == null ||
                    replayObjectData != null && replayObjectData.tickSet.get(replayTickIndex)) {
                playCurrentNestedObjectHitSound();
                listener.onSliderHit(id, 10, null, ballPos, false, bodyColor, GameObjectListener.SLIDER_TICK);

                if (Config.isAnimateFollowCircle() && !isFollowCircleAnimating) {
                    followCircle.clearEntityModifiers();
                    followCircle.registerEntityModifier(Modifiers.scale((float) Math.min(tickInterval, 0.2f), scale * 1.1f, scale, null, EaseQuadOut.getInstance()));
                }

                ticksGot++;
                tickSet.set(replayTickIndex++, true);
            } else {
                listener.onSliderHit(id, -1, null, ballPos, false, bodyColor, GameObjectListener.SLIDER_TICK);
                tickSet.set(replayTickIndex++, false);
            }

            currentNestedObjectIndex++;

            tickSprite.setAlpha(0);
            if (reverse && currentTickSpriteIndex > 0) {
                currentTickSpriteIndex--;
            } else if (!reverse && currentTickSpriteIndex < tickContainer.getChildCount() - 1) {
                currentTickSpriteIndex++;
            }
        }
    }

    private void applyBodyFadeAdjustments(float fadeInDuration) {

        if (GameHelper.isHidden()) {
            // New duration from completed fade in to end (before fading out)
            float fadeOutDuration = (float) beatmapSlider.getDuration() / 1000 + timePreempt - fadeInDuration;

            sliderBody.registerEntityModifier(Modifiers.sequence(
                Modifiers.fadeIn(fadeInDuration),
                Modifiers.fadeOut(fadeOutDuration, null, EaseQuadOut.getInstance())
            ));
        } else {
            sliderBody.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
        }
    }

    private void playCurrentNestedObjectHitSound() {
        listener.playSamples(beatmapSlider.getNestedHitObjects().get(currentNestedObjectIndex));
    }

    @Override
    public void stopAuxiliarySamples() {
        stopSlidingSamples();
    }

    private void playSlidingSamples() {
        if (slidingSamplesPlaying) {
            return;
        }

        slidingSamplesPlaying = true;
        listener.playAuxiliarySamples(beatmapSlider);
    }

    private void stopSlidingSamples() {
        if (!slidingSamplesPlaying) {
            return;
        }

        slidingSamplesPlaying = false;
        listener.stopAuxiliarySamples(beatmapSlider);
    }

    @Override
    public void tryHit(final float dt) {
        if (startHit) {
            return;
        }

        if (isHit() && canBeHit()) {
            listener.registerAccuracy(passedTime);
            startHit = true;
            ticksGot++;
            firstHitAccuracy = (int) (passedTime * 1000);

            if (-passedTime < GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getOverallDifficulty())) {
                playCurrentNestedObjectHitSound();
                listener.onSliderHit(id, 30, null, position,
                        false, bodyColor, GameObjectListener.SLIDER_START);
            } else {
                listener.onSliderHit(id, -1, null, position,
                        false, bodyColor, GameObjectListener.SLIDER_START);
            }

            currentNestedObjectIndex++;
        }

        if (passedTime < 0 && startHit) {
            approachCircle.clearEntityModifiers();
            approachCircle.setAlpha(0);
        }
    }

}