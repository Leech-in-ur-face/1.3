package com.reco1l.data.adapters;
// Created by Reco1l on 05/12/2022, 06:27

import android.view.View;

import androidx.annotation.NonNull;

import com.reco1l.UI;
import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.data.GameNotification;
import com.reco1l.utils.Animation;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osuplus.R;

public class NotificationListAdapter extends BaseAdapter<NotificationListAdapter.ViewHolder, GameNotification> {

    //--------------------------------------------------------------------------------------------/

    public NotificationListAdapter(ArrayList<GameNotification> list) {
        super(list);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getItemLayout() {
        return R.layout.notification;
    }

    @Override
    protected ViewHolder getViewHolder(View root) {
        return new ViewHolder(root);
    }

    //--------------------------------------------------------------------------------------------/

    public static class ViewHolder extends BaseViewHolder<GameNotification> {

        public GameNotification notification;

        private GameNotification.Holder holder;

        //----------------------------------------------------------------------------------------//

        public ViewHolder(@NonNull View root) {
            super(root);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onBind(GameNotification notification, int position) {
            this.notification = notification;
            this.holder = notification.build(root);

            UI.notificationCenter.bindTouch(holder.close, this::onDismiss);
            UI.notificationCenter.bindTouch(holder.body, notification.runOnClick);

            if (notification.hasPriority()) {
                holder.close.setVisibility(View.GONE);
            } else {
                holder.close.setVisibility(View.VISIBLE);
            }
        }

        //----------------------------------------------------------------------------------------//

        private void onDismiss() {
            if (notification.onDismiss != null) {
                notification.onDismiss.run();
            }
            UI.notificationCenter.remove(notification);
        }

        public void notifyUpdate() {
            Animation.of(holder.innerBody)
                    .toX(-50)
                    .toAlpha(0)
                    .runOnEnd(() -> {
                        rebind();

                        Animation.of(holder.innerBody)
                                .fromX(50)
                                .toX(0)
                                .toAlpha(1)
                                .play(120);
                    })
                    .play(120);
        }

        public void notifyProgressUpdate() {
            if (holder.progressIndicator != null) {
                holder.progressIndicator.setProgress(notification.progress);
            }
        }
    }
}
