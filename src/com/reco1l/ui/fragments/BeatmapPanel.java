package com.reco1l.ui.fragments;

import android.animation.ValueAnimator;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.data.BeatmapProperty;
import com.reco1l.utils.Animation;
import com.reco1l.utils.helpers.BeatmapHelper;
import com.reco1l.data.Scoreboard;
import com.reco1l.ui.BaseFragment;
import com.reco1l.tables.Res;
import com.reco1l.interfaces.IGameMods;

import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.helper.DifficultyReCalculator;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 13/9/22 01:22

public final class BeatmapPanel extends BaseFragment implements IGameMods {

    public static BeatmapPanel instance;

    public boolean isOnlineBoard = false;

    private int bodyWidth;
    private CardView banner;
    private LinearLayout songProperties;

    private ImageView songBackground;
    private View body, message, pageContainer, tabIndicator;
    private TextView localTab, globalTab, messageText;

    private boolean
            isTabAnimInProgress = false,
            isBannerExpanded = true;

    // Map properties
    private TrackInfo track;
    private TextView title, artist, mapper, difficulty;

    private final BeatmapProperty.BPM pBPM;
    private final BeatmapProperty.Length pLength;
    private final BeatmapProperty<Integer> pCombo, pCircles, pSliders, pSpinners;
    private final BeatmapProperty<Float> pAR, pOD, pCS, pHP, pStars;
    // End

    private Scoreboard scoreboard;

    private float lastDifficulty = -1;

    //--------------------------------------------------------------------------------------------//

    public BeatmapPanel() {
        super(Screens.Selector);
        pBPM = new BeatmapProperty.BPM();
        pLength = new BeatmapProperty.Length();

        pCombo = new BeatmapProperty<>();
        pCircles = new BeatmapProperty<>();
        pSliders = new BeatmapProperty<>();
        pSpinners = new BeatmapProperty<>();

        pStars = new BeatmapProperty<>();
        pAR = new BeatmapProperty<>();
        pOD = new BeatmapProperty<>();
        pCS = new BeatmapProperty<>();
        pHP = new BeatmapProperty<>();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.beatmap_panel;
    }

    @Override
    protected String getPrefix() {
        return "bp";
    }

    @Override
    protected boolean getConditionToShow() {
        return Game.libraryManager.getSizeOfBeatmaps() != 0;
    }

    //--------------------------------------------------------------------------------------------//
    @Override
    protected void onLoad() {
        closeOnBackgroundClick(false);

        track = Game.musicManager.getTrack();
        bodyWidth = Res.dimen(R.dimen.beatmapPanelContentWidth);
        isBannerExpanded = true;

        if (scoreboard == null) {
            scoreboard = new Scoreboard();
        }
        scoreboard.setContainer(find("scoreboard"));

        body = find("body");

        Animation.of(body)
                .fromX(-bodyWidth)
                .toX(0)
                .fromAlpha(0)
                .toAlpha(1)
                .play(300);

        title = find("title");
        artist = find("artist");
        mapper = find("mapper");
        banner = find("banner");

        difficulty = find("difficulty");
        message = find("messageLayout");
        messageText = find("messageTv");
        songProperties = find("properties");
        pageContainer = find("pageContainer");
        songBackground = find("songBackground");

        localTab = find("localTab");
        globalTab = find("globalTab");
        tabIndicator = find("tabIndicator");

        message.setVisibility(View.GONE);

        bindTouch(globalTab, () -> switchTab(globalTab));
        bindTouch(localTab, () -> switchTab(localTab));

        int max = Res.dimen(R.dimen._84sdp);

        bindTouch(find("expand"), () -> {
            if (isBannerExpanded) {
                ValueAnimator anim = ValueAnimator.ofInt(max, 0);
                anim.setDuration(300);
                anim.addUpdateListener(value -> {
                    songProperties.getLayoutParams().height = (int) value.getAnimatedValue();
                    songProperties.requestLayout();
                });
                anim.start();
                isBannerExpanded = false;
            } else {
                ValueAnimator anim = ValueAnimator.ofInt(0, max);
                anim.setDuration(300);
                anim.addUpdateListener(value -> {
                    songProperties.getLayoutParams().height = (int) value.getAnimatedValue();
                    songProperties.requestLayout();
                });
                anim.start();
                isBannerExpanded = true;
            }
        });

        pStars.view = find("stars");
        pStars.format = val -> GameHelper.Round(val, 2);
        pStars.allowColorChange = false;

        pBPM.view = find("beatspersecond");
        pLength.view = find("length");

        pCombo.view = find("combo");
        pCircles.view = find("circles");
        pSliders.view = find("sliders");
        pSpinners.view = find("spinners");

        pAR.view = find("ar");
        pOD.view = find("od");
        pCS.view = find("cs");
        pHP.view = find("hp");

        pAR.format = val -> GameHelper.Round(val, 2);
        pOD.format = val -> GameHelper.Round(val, 2);
        pCS.format = val -> GameHelper.Round(val, 2);
        pHP.format = val -> GameHelper.Round(val, 2);

        updateProperties(track);
        switchTab(localTab);
    }

    //--------------------------------------------------------------------------------------------//

    // Code transformed from old SongMenu
    private void updateDimensionProperties() {
        if (track == null)
            return;

        EnumSet<GameMod> mod = Game.modMenu.getMod();

        pOD.set(track.getOverallDifficulty());
        pAR.set(track.getApproachRate());
        pCS.set(track.getCircleSize());
        pHP.set(track.getHpDrain());

        pLength.set(track.getMusicLength());
        pBPM.set(track.getBpmMin(), track.getBpmMax());

        if (mod.contains(EZ)) {
            pAR.value *= 0.5f;
            pOD.value *= 0.5f;
            pHP.value *= 0.5f;
            pCS.value -= 1f;
        }
        if (mod.contains(HR)) {
            pAR.value = Math.min(pAR.value * 1.4f, 10);
            pOD.value *= 1.4f;
            pHP.value *= 1.4f;
            pCS.value += 1f;
        }
        if (mod.contains(REZ)) {
            if (mod.contains(EZ)) {
                pAR.value *= 2f;
                pAR.value -= 0.5f;
            }
            pAR.value -= 0.5f;
            if (Game.modMenu.getChangeSpeed() != 1) {
                pAR.value -= Game.modMenu.getSpeed() - 1.0f;
            }
            else if (mod.contains(DT) || mod.contains(NC)) {
                pAR.value -= 0.5f;
            }
            pOD.value *= 0.5f;
            pHP.value *= 0.5f;
            pCS.value -= 1f;
        }
        if (mod.contains(SC)) {
            pCS.value += 4f;
        }

        if (Game.modMenu.getChangeSpeed() != 1) {
            float speed = Game.modMenu.getSpeed();
            pBPM.multiply(speed);
            pLength.value = (long) (pLength.value / speed);
        } else {
            if (mod.contains(DT) || mod.contains(NC)) {
                pBPM.multiply(1.5f);
                pLength.value = (long) (pLength.value * (2 / 3f));
            }
            if (mod.contains(HT)) {
                pBPM.multiply(0.7f);
                pLength.value = (long) (pLength.value * (4 / 3f));
            }
        }

        pAR.value = Math.min(13.f, pAR.value);
        pOD.value = Math.min(10.f, pOD.value);
        pCS.value = Math.min(15.f, pCS.value);
        pHP.value = Math.min(10.f, pHP.value);

        if (Game.modMenu.getChangeSpeed() != 1) {
            float speed = Game.modMenu.getSpeed();
            pAR.value = GameHelper.Round(GameHelper.ms2ar(GameHelper.ar2ms(pAR.value) / speed), 2);
            pOD.value = GameHelper.Round(GameHelper.ms2od(GameHelper.od2ms(pOD.value) / speed), 2);
        } else if (mod.contains(DT) || mod.contains(NC)) {
            pAR.value = GameHelper.Round(GameHelper.ms2ar(GameHelper.ar2ms(pAR.value) * 2 / 3), 2);
            pOD.value = GameHelper.Round(GameHelper.ms2od(GameHelper.od2ms(pOD.value) * 2 / 3), 2);
        } else if (mod.contains(HT)) {
            pAR.value = GameHelper.Round(GameHelper.ms2ar(GameHelper.ar2ms(pAR.value) * 4 / 3), 2);
            pOD.value = GameHelper.Round(GameHelper.ms2od(GameHelper.od2ms(pOD.value) * 4 / 3), 2);
        }
        if (Game.modMenu.isEnableForceAR()) {
            pAR.value = Game.modMenu.getForceAR();
        }

        if (isAdded()) {
            pOD.update();
            pAR.update();
            pCS.update();
            pHP.update();

            pLength.update();
            pBPM.update();
        }
    }

    public void updateProperties(TrackInfo track) {
        this.track = track;
        if (track == null || !isLoaded())
            return;

        Game.activity.runOnUiThread(() -> {

            pStars.set(track.getDifficulty());
            pCircles.set(track.getHitCircleCount());
            pSpinners.set(track.getSpinnerCount());
            pSliders.set(track.getSliderCount());
            pCombo.set(track.getMaxCombo());

            updateDimensionProperties();

            new Thread(() -> {
                DifficultyReCalculator math = new DifficultyReCalculator();
                pStars.value = math.recalculateStar(track, math.getCS(track), Game.modMenu.getSpeed());
                Game.activity.runOnUiThread(pStars::update);
            }).start();

            if (!isAdded())
                return;

            title.setText(BeatmapHelper.getTitle(track));
            artist.setText("by " + BeatmapHelper.getArtist(track));
            mapper.setText(track.getCreator());
            difficulty.setText(track.getMode());

            if (track.getBackground() != null) {
                songBackground.setVisibility(View.VISIBLE);
                songBackground.setImageDrawable(BeatmapHelper.getBackground(track));
                find("gradient").setAlpha(1);
            } else {
                songBackground.setVisibility(View.INVISIBLE);
                find("gradient").setAlpha(0);
            }

            pStars.update();
            pCircles.update();
            pSpinners.update();
            pSliders.update();
            pCombo.update();

            int darkerColor = BeatmapHelper.Palette.getDarkerColor(pStars.value);
            int textColor = BeatmapHelper.Palette.getTextColor(pStars.value);
            int color = BeatmapHelper.Palette.getColor(pStars.value);

            if (lastDifficulty == -1) {
                pStars.view.getBackground().setTint(color);
                pStars.view.setTextColor(textColor);
                banner.setCardBackgroundColor(darkerColor);

                lastDifficulty = pStars.value;
                return;
            }

            int lastDarkerColor = BeatmapHelper.Palette.getDarkerColor(lastDifficulty);
            int lastTextColor = BeatmapHelper.Palette.getTextColor(lastDifficulty);
            int lastColor = BeatmapHelper.Palette.getColor(lastDifficulty);
            lastDifficulty = pStars.value;

            Animation.ofColor(lastTextColor, textColor)
                    .runOnUpdate(value -> {
                        pStars.view.setTextColor((int) value);
                        if (pStars.view.getCompoundDrawablesRelative()[0] != null) {
                            pStars.view.getCompoundDrawablesRelative()[0].setTint((int) value);
                        }
                    }).play(500);


            Animation.ofColor(lastColor, color)
                    .runOnUpdate(value -> pStars.view.getBackground().setTint((int) value))
                    .play(500);

            Animation.ofColor(lastDarkerColor, darkerColor)
                    .runOnUpdate(value -> {
                        banner.setCardBackgroundColor((int) value);
                        mapper.getBackground().setTint((int) value);
                    })
                    .play(500);
        });
    }

    private void switchTab(View button) {
        if (isTabAnimInProgress || tabIndicator.getTranslationX() == button.getX())
            return;

        boolean toRight = tabIndicator.getTranslationX() < button.getX();

        Animation.of(tabIndicator)
                .toX(button.getX())
                .play(150);

        Animation.of(pageContainer)
                .toX(toRight ? -60 : 60)
                .toAlpha(0)
                .runOnStart(() -> isTabAnimInProgress = true)
                .play(200);

        Animation.of(pageContainer)
                .fromX(toRight ? 60 : -60)
                .toX(0)
                .toAlpha(1)
                .runOnEnd(() -> isTabAnimInProgress = false)
                .delay(200)
                .play(200);

        pageContainer.postDelayed(() -> {
            isOnlineBoard = button == globalTab;
            updateScoreboard();
        }, 200);
    }

    public void updateScoreboard() {
        if (!isAdded() || !isLoaded())
            return;

        Game.activity.runOnUiThread(() -> {
            if (scoreboard.loadScores(Game.musicManager.getTrack(), isOnlineBoard)) {
                message.setVisibility(View.GONE);
            } else {
                message.setVisibility(View.VISIBLE);
                messageText.setText(scoreboard.errorMessage);
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        if (!isAdded())
            return;
        scoreboard.setContainer(null);

        Animation.of(body)
                .toX(-bodyWidth)
                .toTopMargin((int) Res.dimen(R.dimen.topBarHeight))
                .toAlpha(0)
                .runOnEnd(super::close)
                .play(300);
    }

    //--------------------------------------------------------------------------------------------//
    // Temporal workaround until DuringGameScoreBoard gets replaced (old UI)

    public Scoreboard.Item[] getBoard() {
        return scoreboard.boardScores.toArray(new Scoreboard.Item[0]);
    }

    //--------------------------------------------------------------------------------------------//
}
