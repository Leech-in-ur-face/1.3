package com.reco1l.data;

import androidx.annotation.XmlRes;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;

import com.reco1l.global.Game;
import com.reco1l.preference.ButtonPreference;
import com.reco1l.preference.FieldPreference;
import com.reco1l.tables.DialogTable;
import com.reco1l.ui.BasePreferenceFragment;
import com.reco1l.ui.custom.Dialog;

import java.io.File;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.R;

public final class Settings {

    //--------------------------------------------------------------------------------------------//

    public static abstract class Wrapper {

        public abstract @XmlRes int getPreferenceXML();

        public void onLoad(BasePreferenceFragment parent) {}

    }

    //--------------------------------------------------------------------------------------------//

    public static class General extends Wrapper {

        public int getPreferenceXML() {
            return R.xml.settings_general;
        }

    }

    //--------------------------------------------------------------------------------------------//

    public static class Appearance extends Wrapper {

        public int getPreferenceXML() {
            return R.xml.settings_appearance;
        }

        @Override
        public void onLoad(BasePreferenceFragment f) {
            ButtonPreference skin = f.find("skinPath");
            skin.setOnPreferenceClickListener(p -> new Dialog(DialogTable.skins()).show());
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class Gameplay extends Wrapper {

        public int getPreferenceXML() {
            return R.xml.settings_gameplay;
        }

    }

    //--------------------------------------------------------------------------------------------//

    public static class Graphics extends Wrapper {

        public int getPreferenceXML() {
            return R.xml.settings_graphics;
        }

    }

    //--------------------------------------------------------------------------------------------//

    public static class Sounds extends Wrapper {

        public int getPreferenceXML() {
            return R.xml.settings_sounds;
        }

        @Override
        public void onLoad(BasePreferenceFragment f) {
            SeekBarPreference bgmVolume = f.find("bgmvolume");

            if (bgmVolume != null) {
                bgmVolume.setOnPreferenceChangeListener((p, val) -> {
                    Game.musicManager.setVolume((int) val / 100f);
                    return true;
                });
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class Library extends Wrapper {

        public int getPreferenceXML() {
            return R.xml.settings_library;
        }

        @Override
        public void onLoad(BasePreferenceFragment f) {
            Preference clearProperties = f.find("clear_properties");
            Preference clearCache = f.find("clear");

            if (clearProperties != null) {
                clearProperties.setOnPreferenceClickListener(p -> {
                    Game.libraryManager.clearCache();
                    return true;
                });
            }

            if (clearCache != null) {
                clearCache.setOnPreferenceClickListener(p -> {
                    Game.propertiesLibrary.clear(Game.activity);
                    return true;
                });
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class Advanced extends Wrapper {

        public int getPreferenceXML() {
            return R.xml.settings_advanced;
        }

        @Override
        public void onLoad(BasePreferenceFragment f) {
            FieldPreference path = f.find("corePath");

            if (path != null) {
                path.setDefaultValue(Config.getDefaultCorePath());
                path.setText(Config.getCorePath());

                path.setOnFocusLostListener(() -> {
                    if (path.getText().trim().length() == 0) {
                        path.setText(Config.getCorePath());
                    }

                    File file = new File(path.getText());
                    if (!file.exists() && !file.mkdirs()) {
                        path.setText(Config.getCorePath());
                    }
                });

                path.setOnPreferenceChangeListener((p, newValue) -> {
                    Config.loadPaths();
                    return false;
                });
            }
        }
    }
}
