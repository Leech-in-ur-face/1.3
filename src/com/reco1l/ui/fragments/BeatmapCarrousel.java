package com.reco1l.ui.fragments;

import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.management.BeatmapCollection;
import com.reco1l.enums.Screens;
import com.reco1l.data.adapters.BeatmapListAdapter;
import com.reco1l.ui.BaseFragment;
import com.reco1l.view.CarrouselRecyclerView;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/8/22 00:31

public final class BeatmapCarrousel extends BaseFragment implements BeatmapCollection.Listener {

    public static BeatmapCarrousel instance;

    public CarrouselRecyclerView recyclerView;

    private BeatmapListAdapter adapter;

    //--------------------------------------------------------------------------------------------//

    public BeatmapCarrousel() {
        super(Screens.Selector);
        Game.beatmapCollection.addListener(this);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.beatmap_carrousel;
    }

    @Override
    protected String getPrefix() {
        return "bl";
    }

    @Override
    protected boolean getConditionToShow() {
        return Game.libraryManager.getSizeOfBeatmaps() != 0;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        recyclerView = find("recycler");

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), VERTICAL, false));
        recyclerView.setYOffset(-UI.topBar.getHeight());
        recyclerView.setAdapter(adapter);

        restoreSelection();
    }

    @Override
    public void onCollectionChange(ArrayList<BeatmapInfo> list) {
        if (adapter == null) {
            adapter = new BeatmapListAdapter(list);
            adapter.setSelectionListener(pos ->
                    recyclerView.scrollToPosition(pos)
            );
        }
        adapter.setData(list);
    }

    public void restoreSelection() {
        if (adapter != null) {
            adapter.select(Game.musicManager.getTrack().getBeatmap());
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        super.close();
    }
}
