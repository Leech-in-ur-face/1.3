package com.reco1l.ui.fragments;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.edlplan.ui.TriangleEffectView;
import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.UI;
import com.reco1l.utils.Animation;
import com.reco1l.tables.AnimationTable;
import com.reco1l.management.KeyInputManager;
import com.reco1l.utils.helpers.BeatmapHelper;
import com.reco1l.tables.DialogTable;
import com.reco1l.ui.BaseFragment;
import com.reco1l.tables.Res;
import com.reco1l.utils.helpers.OnlineHelper;
import com.reco1l.view.IconButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 26/6/22 21:20

public final class TopBar extends BaseFragment {

    public static TopBar instance;

    public UserBox userBox;
    public MusicButton musicButton;

    private View back, body;
    private LinearLayout container, buttonsContainer;

    private final Map<Screens, ArrayList<IconButton>> buttons;

    private int barHeight;

    //--------------------------------------------------------------------------------------------//

    public TopBar() {
        super(Screens.Selector, Screens.Summary, Screens.Loader);
        buttons = new HashMap<>();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "tb";
    }

    @Override
    protected int getLayout() {
        return R.layout.top_bar;
    }

    @Override
    protected boolean isOverlay() {
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        barHeight = (int) Res.dimen(R.dimen.topBarHeight);

        body = find("body");

        Animation.of(body)
                .fromY(-barHeight)
                .toY(0)
                .play(200);

        Game.platform.animate(true, true)
                .toTopMargin(barHeight)
                .play(200);

        musicButton = new MusicButton(this);
        userBox = new UserBox(this);

        buttonsContainer = find("buttons");
        container = find("container");
        back = find("back");


        bindTouch(find("inbox"), UI.notificationCenter::altShow);
        bindTouch(find("settings"), UI.settingsPanel::altShow);
        bindTouch(back, KeyInputManager::performBack);

        userBox.loadUserData(false);

        reloadButtons(Game.engine.getScreen());
    }

    @Override
    protected void onScreenChange(Screens lastScreen, Screens newScreen) {
        if (isAdded()) {
            reloadButtons(newScreen);
        }
    }

    public void reloadButtons(Screens current) {
        if (container == null || buttonsContainer == null) {
            return;
        }

        Animation.of(container)
                .toX(-60)
                .toAlpha(0)
                .runOnEnd(() -> {
                    buttonsContainer.removeAllViews();

                    if (current == Screens.Main) {
                        musicButton.setVisibility(Game.libraryManager.getSizeOfBeatmaps() != 0);
                        back.setVisibility(View.GONE);
                    } else {
                        musicButton.setVisibility(false);
                        if (current != Screens.Loader) {
                            back.setVisibility(View.VISIBLE);
                        }
                    }

                    ArrayList<IconButton> toAdd = buttons.get(current);
                    if (toAdd != null) {
                        for (IconButton button : toAdd) {
                            buttonsContainer.addView(button);
                            bindTouch(button, button.getTouchListener());
                        }
                    }

                    Animation.of(container)
                            .toX(0)
                            .toAlpha(1)
                            .play(200);
                })
                .play(200);
    }

    @Override
    public void close() {
        if (isAdded()) {

            Animation.of(body)
                    .toY(-barHeight)
                    .runOnEnd(super::close)
                    .play(200);

            Game.platform.animate(true, true)
                    .toTopMargin(0)
                    .play(200);
        }
    }

    @Override
    public void show() {
        if (Game.engine.getScreen() == Screens.Loader) {
            if (Game.loaderScene.isImmersive()) {
                return;
            }
        }
        super.show();
    }
    //--------------------------------------------------------------------------------------------//

    public void addButton(Screens screen, IconButton button) {
        buttons.computeIfAbsent(screen, k -> new ArrayList<>());

        ArrayList<IconButton> list = buttons.get(screen);

        if (list != null) {
            list.add(button);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class MusicButton {

        private final TopBar parent;
        private final View view, body, arrow;
        private final TextView text;

        //----------------------------------------------------------------------------------------//

        public MusicButton(TopBar parent) {
            this.parent = parent;

            view = parent.find("music");
            body = parent.find("musicBody");
            arrow = parent.find("musicArrow");
            text = parent.find("musicText");

            parent.bindTouch(view, UI.musicPlayer::altShow);

            if (Game.libraryManager.getBeatmap() != null) {
                text.setText(BeatmapHelper.getTitle(Game.libraryManager.getBeatmap().getTrack(0)));
            }
        }

        //----------------------------------------------------------------------------------------//

        public void changeMusic(BeatmapInfo beatmap) {
            if (parent.isAdded()) {
                AnimationTable.textChange(text, BeatmapHelper.getTitle(beatmap));
            }
        }

        public void animateButton(boolean show) {
            Animation bodyAnim = Animation.of(body);
            Animation arrowAnim = Animation.of(arrow);

            if (show) {
                bodyAnim.fromY(0)
                        .toY(-10)
                        .fromAlpha(1)
                        .toAlpha(0);

                arrowAnim.fromY(10)
                        .toY(0)
                        .fromAlpha(0)
                        .toAlpha(1);
            } else {
                bodyAnim.fromY(10)
                        .toY(0)
                        .fromAlpha(0)
                        .toAlpha(1);

                arrowAnim.fromY(0)
                        .toY(-10)
                        .fromAlpha(1)
                        .toAlpha(0);
            }
            arrowAnim.duration(150);

            bodyAnim.runOnEnd(arrowAnim::play);
            bodyAnim.play(150);
        }

        protected void setVisibility(boolean bool) {
            view.setVisibility(bool ? View.VISIBLE : View.GONE);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class UserBox {

        private final TopBar parent;
        private final ImageView avatar;
        private final TextView rank, name;

        //----------------------------------------------------------------------------------------//

        public UserBox(TopBar parent) {
            this.parent = parent;

            View body = parent.find("userBox");
            rank = parent.find("playerRank");
            name = parent.find("playerName");
            avatar = parent.find("avatar");

            TriangleEffectView triangles = parent.find("userBoxTriangles");
            triangles.setTriangleColor(0xFFFFFFFF);

            parent.bindTouch(body, UI.userProfile::altShow);
        }

        //----------------------------------------------------------------------------------------//

        public void loadUserData(boolean clear) {
            if (!parent.isAdded())
                return;

            AnimationTable.fadeOutIn(avatar, () -> avatar.setImageResource(R.drawable.default_avatar));

            AnimationTable.textChange(rank, Res.str(R.string.top_bar_offline));
            AnimationTable.textChange(name, Config.getLocalUsername());

            if (Game.onlineManager.isStayOnline() && !clear) {
                AnimationTable.textChange(name, Game.onlineManager.getUsername());
                AnimationTable.textChange(rank, "#" + Game.onlineManager.getRank());

                AnimationTable.fadeOutIn(avatar, () -> {
                    if (OnlineHelper.getPlayerAvatar() != null) {
                        avatar.setImageDrawable(OnlineHelper.getPlayerAvatar());
                    }
                });
            }
        }
    }
}
