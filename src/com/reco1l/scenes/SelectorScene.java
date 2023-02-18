package com.reco1l.scenes;

// Created by Reco1l on 26/9/22 17:40

import android.widget.LinearLayout;

import com.reco1l.global.Game;
import com.reco1l.global.UI;
import com.reco1l.global.Scenes;
import com.reco1l.interfaces.ITask;
import com.reco1l.tables.NotificationTable;
import com.reco1l.utils.Animation;
import com.reco1l.utils.execution.Async;
import com.reco1l.view.IconButton;

import org.anddev.andengine.entity.scene.Scene;

import java.util.Random;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.OnlineManagerException;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.R;

public class SelectorScene extends BaseScene {

    public static final SelectorScene instance = new SelectorScene();

    public final IconButton
            modsButton,
            searchButton,
            randomButton;

    //--------------------------------------------------------------------------------------------//

    public SelectorScene() {
        super();

        modsButton = new IconButton(context());
        searchButton = new IconButton(context());
        randomButton = new IconButton(context());
    }

    //--------------------------------------------------------------------------------------------//

    public void onTrackSelect(TrackInfo track) {
        if (Game.musicManager.getTrack() == track) {
            return;
        }
        Game.musicManager.change(track);
        Game.globalManager.setSelectedTrack(track);
    }

    public void onAudioChange(TrackInfo track) {
        if (track == null) {
            return;
        }
        Game.musicManager.setVolume(0);
        Game.musicManager.setPosition(track.getPreviewTime());

        Animation.ofFloat(0, Config.getBgmVolume())
                .runOnUpdate(v -> Game.musicManager.setVolume((float) v))
                .play(300);
    }

    @Override
    public void onButtonContainerChange(LinearLayout layout) {
        modsButton.setTouchListener(() ->
                modsButton.setSelected(UI.modMenu.alternate())
        );
        modsButton.setIcon(R.drawable.v18_tune);

        searchButton.setTouchListener(() ->
                modsButton.setSelected(UI.filterBar.alternate())
        );
        searchButton.setIcon(R.drawable.v18_search);

        randomButton.setIcon(R.drawable.v18_random);
        randomButton.setTouchListener(this::random);

        layout.addView(modsButton);
        layout.addView(searchButton);
        layout.addView(randomButton);
    }

    //--------------------------------------------------------------------------------------------//

    public void loadScore(int id, String player) {
        TrackInfo track = Game.musicManager.getTrack();

        Scenes.loader.async(new ITask() {

            private StatisticV2 mStats;
            private String mReplay;

            public void run() {
                if (!Game.boardManager.isOnlineBoard()) {
                    mStats = Game.scoreLibrary.getScore(id);
                    mReplay = mStats.getReplayName();
                    return;
                }

                String pack;
                try {
                    pack = Game.onlineManager.getScorePack(id);
                }
                catch (OnlineManagerException e) {
                    NotificationTable.exception(e);
                    return;
                }

                String[] params = pack.split("\\s+");
                if (params.length >= 11) {
                    mStats = new StatisticV2(params);
                    mStats.setPlayerName(player);
                }
                mReplay = OnlineManager.getReplayURL(id);
            }

            public void onComplete() {
                if (mStats == null) {
                    show();
                    return;
                }
                Scenes.summary.load(mStats, track, mReplay, true);
            }
        });
    }

    public void random() {
        Random r = new Random();

        int bBound = Game.libraryManager.getSizeOfBeatmaps();
        int bIndex = r.nextInt(bBound);

        BeatmapInfo beatmap = Game.libraryManager.getBeatmapByIndex(bIndex);

        int tBound = beatmap.getTracks().size();
        int tIndex = r.nextInt(tBound);

        TrackInfo track = beatmap.getTrack(tIndex);

        onTrackSelect(track);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onMusicChange(TrackInfo newTrack, boolean isSameAudio) {
        super.onMusicChange(newTrack, isSameAudio);

        if (!isSameAudio) {
            onAudioChange(newTrack);
        }
    }

    @Override
    public void onMusicEnd() {
        Game.musicManager.reset();
        onAudioChange(Game.musicManager.getTrack());
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public boolean onBackPress() {
        Game.engine.setScene(Scenes.main);
        return true;
    }

    @Override
    public void onSceneChange(Scene lastScene, Scene newScene) {
        if (newScene == this) {
            Game.musicManager.play();
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void show() {
        Async.run(() -> {
            Game.activity.checkNewBeatmaps();

            if (!Game.libraryManager.loadLibraryCache(Game.activity, true)) {
                Game.libraryManager.scanLibrary(Game.activity);
            }
        });
        super.show();
    }
}
