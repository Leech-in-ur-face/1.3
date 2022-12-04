package com.reco1l.ui.fragments;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.TriangleEffectView;
import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.UI;
import com.reco1l.utils.Animation;
import com.reco1l.utils.AnimationTable;
import com.reco1l.utils.KeyInputHandler;
import com.reco1l.utils.helpers.BeatmapHelper;
import com.reco1l.ui.data.DialogTable;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.AnimationOld;
import com.reco1l.utils.Resources;
import com.reco1l.view.BarButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 26/6/22 21:20

public class TopBar extends UIFragment {

    public static TopBar instance;

    public UserBox userBox;
    public MusicButton musicButton;

    private View back, body;
    private TextView author;
    private LinearLayout container, buttonsContainer;

    private final Map<Screens, ArrayList<BarButton>> buttons;

    private int barHeight;

    //--------------------------------------------------------------------------------------------//

    public TopBar() {
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
    protected Screens[] getParents() {
        return new Screens[]{Screens.Selector, Screens.Summary};
    }

    @Override
    protected boolean isOverlay() {
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        setDismissMode(false, false);
        barHeight = (int) Resources.dimen(R.dimen.topBarHeight);

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
        author = find("author");
        back = find("back");

        if (library.getSizeOfBeatmaps() <= 0) {
            musicButton.setVisibility(false);
        }

        author.setText(String.format("osu!droid %s", BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_TYPE + ")"));

        bindTouchListener(find("inbox"), UI.notificationCenter::altShow);
        bindTouchListener(find("settings"), UI.settingsPanel::altShow);
        bindTouchListener(back, KeyInputHandler::performBack);

        bindTouchListener(author, () -> new Dialog(DialogTable.author()).show());

        userBox.loadUserData(false);

        reloadButtons(Game.engine.currentScreen);
    }

    @Override
    protected void onScreenChange(Screens lastScreen, Screens newScreen) {
        if (isShowing) {
            reloadButtons(newScreen);
            showAuthorText(newScreen == Screens.Main);
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
                        musicButton.setVisibility(true);
                        back.setVisibility(View.GONE);
                    } else {
                        musicButton.setVisibility(false);
                        back.setVisibility(View.VISIBLE);
                    }

                    ArrayList<BarButton> toAdd = buttons.get(current);
                    if (toAdd != null) {
                        for (BarButton button : toAdd) {
                            buttonsContainer.addView(button);
                            bindTouchListener(button, button.getTouchListener());
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
        if (isShowing) {
            showAuthorText(false);

            Animation.of(body)
                    .toY(-barHeight)
                    .runOnEnd(super::close)
                    .play(200);

            Game.platform.animate(true, true)
                    .toTopMargin(0)
                    .play(200);
        }
    }

    //--------------------------------------------------------------------------------------------//

    private void showAuthorText(boolean bool) {
        if (author == null)
            return;

        Animation anim = Animation.of(author);

        if (bool && author.getVisibility() != View.VISIBLE) {
            anim.toAlpha(1);
            anim.toY(0);
            anim.runOnStart(() -> author.setVisibility(View.VISIBLE));
        } else {
            anim.toAlpha(0);
            anim.toY(50);
            anim.runOnEnd(() -> author.setVisibility(View.GONE));
        }
        anim.play(200);
    }

    public void addButton(Screens screen, BarButton button) {
        if (buttons.get(screen) == null) {
            buttons.put(screen, new ArrayList<>());
        }
        ArrayList<BarButton> list = buttons.get(screen);

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

            parent.bindTouchListener(view, UI.musicPlayer::altShow);
        }

        //----------------------------------------------------------------------------------------//

        public void changeMusic(BeatmapInfo beatmap) {
            if (parent.isShowing) {
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

            parent.bindTouchListener(body, UI.userProfile::altShow);
        }

        //----------------------------------------------------------------------------------------//

        public void loadUserData(boolean clear) {
            if (!parent.isShowing)
                return;

            AnimationTable.fadeOutIn(avatar, () -> avatar.setImageResource(R.drawable.default_avatar));

            AnimationTable.textChange(rank, Resources.str(R.string.top_bar_offline));
            AnimationTable.textChange(name, Config.getLocalUsername());

            if (online.isStayOnline() && !clear) {
                AnimationTable.textChange(name, online.getUsername());
                AnimationTable.textChange(rank, "#" + online.getRank());

                AnimationTable.fadeOutIn(avatar, () -> {
                    if (onlineHelper.getPlayerAvatar() != null) {
                        avatar.setImageDrawable(onlineHelper.getPlayerAvatar());
                    }
                });
            }
        }
    }
}
