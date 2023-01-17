package com.reco1l.tables;

import android.content.Intent;
import android.widget.TextView;

import com.reco1l.Game;
import com.reco1l.ui.SimpleFragment;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.custom.DialogBuilder;

import ru.nsu.ccfit.zuev.osu.MainActivity;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 25/7/22 22:30

public class DialogTable {

    // Builder for Author dialog
    //--------------------------------------------------------------------------------------------//
    public static DialogBuilder author() {
        DialogBuilder builder = new DialogBuilder();

        builder.title = "Information";
        builder.customFragment = new AuthorFragment();
        //builder.setCloseMode(true);
        builder.addButton("Close", Dialog::close);

        return builder;
    }

    public static class AuthorFragment extends SimpleFragment {

        public AuthorFragment() {
            super(R.layout.layout_author);
        }

        protected void onLoad() {
            TextView versionTv = find(R.id.d_author_version);
            versionTv.setText(String.format("%s", BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_TYPE + ")"));
        }
    }

    // Builder for restart dialog
    //--------------------------------------------------------------------------------------------//
    public static DialogBuilder restart() {
        DialogBuilder builder = new DialogBuilder();

        builder.title = "Restart";
        builder.message = "The game will be restarted to apply changes";
        //builder.setCloseMode(false);
        builder.addButton("Accept", dialog -> {
            Intent intent = new Intent(Game.activity, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Game.activity.startActivity(intent);
            System.exit(0);
        });

        return builder;
    }

    // Builder for restart dialog
    //--------------------------------------------------------------------------------------------//
    public static DialogBuilder exit() {
        DialogBuilder builder = new DialogBuilder();

        builder.title = "Exit";
        builder.message = "Are you sure you want to exit the game?";
        //builder.setCloseMode(true);
        builder.addButton("Accept", dialog -> {
            dialog.close();
            Game.exit();
        });
        builder.addButton("Cancel", Dialog::close);

        return builder;
    }

    // Builder for auto-clicker dialog
    //--------------------------------------------------------------------------------------------//
    public static DialogBuilder auto_clicker() {
        DialogBuilder builder = new DialogBuilder();

        builder.title = "Warning";
        builder.message = Res.str(R.string.message_autoclicker_detected);
        //builder.setCloseMode(false);
        builder.addButton("Exit", dialog -> {
            dialog.close();
            Game.forcedExit();
        });

        return builder;
    }
}
