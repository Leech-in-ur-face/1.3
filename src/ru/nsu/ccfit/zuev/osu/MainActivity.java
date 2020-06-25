package ru.nsu.ccfit.zuev.osu;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.edlplan.ui.ActivityOverlay;
import com.tencent.bugly.Bugly;
import com.umeng.analytics.MobclickAgent;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.camera.SmoothCamera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.extension.input.touch.controller.MultiTouch;
import org.anddev.andengine.extension.input.touch.controller.MultiTouchController;
import org.anddev.andengine.extension.input.touch.exception.MultiTouchException;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.view.RenderSurfaceView;
import org.anddev.andengine.sensor.accelerometer.AccelerometerData;
import org.anddev.andengine.sensor.accelerometer.IAccelerometerListener;
import org.anddev.andengine.ui.activity.BaseGameActivity;
import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import ru.nsu.ccfit.zuev.audio.BassAudioPlayer;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SaveServiceObject;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;
import ru.nsu.ccfit.zuev.osu.game.SpritePool;
import ru.nsu.ccfit.zuev.osu.helper.InputManager;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.menu.FilterMenu;
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osu.menu.SplashScene;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

public class MainActivity extends BaseGameActivity implements
        IAccelerometerListener {
    public static SongService songService;
    public ServiceConnection connection;
    public BroadcastReceiver onNotifyButtonClick;
    private PowerManager.WakeLock wakeLock = null;
    private String beatmapToAdd = null;
    private SaveServiceObject saveServiceObject;
    private IntentFilter filter;
    private boolean willReplay = false;

    public Engine onLoadEngine() {
        checkPermissions();
//        Debug.setDebugLevel(Debug.DebugLevel.NONE);
        Config.loadConfig(this);
        StringTable.setContext(this);
        ToastLogger.init(this);
        SyncTaskManager.getInstance().init(this);
        InputManager.setContext(this);
        // Shape.setUseDither(Config.isUseDither());
        File dir = new File(Config.getBeatmapPath());
        // Creating Osu directory if it doesn't exist
        if (dir.exists() == false) {
            if (dir.mkdirs() == false) {
                Config.setBeatmapPath(Config.getCorePath() + "Songs/");
                dir = new File(Config.getBeatmapPath());
                if (dir.exists() == false && dir.mkdirs() == false) {
                    ToastLogger.showText(StringTable.format(
                            R.string.message_error_createdir, dir.getPath()),
                            true);
                } else {
                    final SharedPreferences prefs = PreferenceManager
                            .getDefaultSharedPreferences(this);
                    final SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("directory", dir.getPath());
                    editor.commit();
                }

            }
            final File nomedia = new File(dir.getParentFile(), ".nomedia");
            try {
                nomedia.createNewFile();
            } catch (final IOException e) {
                Debug.e("LibraryManager: " + e.getMessage(), e);
            }
        }

        final File skinDir = new File(Config.getCorePath() + "/Skin");
        // Creating Osu/Skin directory if it doesn't exist
        if (skinDir.exists() == false) {
            skinDir.mkdirs();
        }

        // 初始化BuglySDK
        Bugly.init(getApplicationContext(), "d1e89e4311", false);
        OnlineManager.getInstance().Init(getApplicationContext());

        final DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
/*        final double screenSize = Math.sqrt(Utils.sqr(dm.widthPixels / dm.xdpi)
                + Utils.sqr(dm.heightPixels / dm.ydpi));*/
        double screenInches = Math.sqrt(Math.pow(dm.heightPixels, 2) + Math.pow(dm.widthPixels, 2)) / (dm.density * 160.0f);
        Debug.i("screen inches: " + screenInches);
        Config.setScaleMultiplier((float) ((11 - 5.2450170716245195) / 5));

        Config.setTextureQuality(1);
        final PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = manager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                "osudroid:osu");

        Camera mCamera = new SmoothCamera(0, 0, Config.getRES_WIDTH(),
                Config.getRES_HEIGHT(), 0, 1800, 1);
        final EngineOptions opt = new EngineOptions(true,
                null, new RatioResolutionPolicy(
                Config.getRES_WIDTH(), Config.getRES_HEIGHT()),
                mCamera);
        opt.setNeedsMusic(true);
        opt.setNeedsSound(true);
        opt.getRenderOptions().disableExtensionVertexBufferObjects();
        opt.getTouchOptions().enableRunOnUpdateThread();
        final Engine engine = new Engine(opt);

        if (Config.isMultitouch()) {
            try {
                if (MultiTouch.isSupported(this)) {
                    engine.setTouchController(new MultiTouchController());
                } else {
                    ToastLogger.showText(
                            StringTable.get(R.string.message_error_multitouch),
                            false);
                }
            } catch (final MultiTouchException e) {
                ToastLogger.showText(
                        StringTable.get(R.string.message_error_multitouch),
                        false);
            }
        }
        GlobalManager.getInstance().setCamera(mCamera);
        GlobalManager.getInstance().setEngine(engine);
        GlobalManager.getInstance().setMainActivity(this);
        return GlobalManager.getInstance().getEngine();
    }

    private void initPreferences() {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        if (prefs.getString("playername", "").equals("")) {
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString("playername", "Guest");
            editor.commit();

            final AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle(StringTable.get(R.string.dialog_playername_title));
            alert.setMessage(StringTable
                    .get(R.string.dialog_playername_message));

            final EditText input = new EditText(this);
            input.setText("Guest");
            alert.setView(input);

            alert.setPositiveButton(StringTable.get(R.string.dialog_ok),
                    new DialogInterface.OnClickListener() {

                        public void onClick(final DialogInterface dialog,
                                            final int whichButton) {
                            final String value = input.getText().toString();
                            editor.putString("playername", value);
                            editor.commit();
                        }
                    });

            alert.show();
        }

        if (prefs.getBoolean("qualitySet", false) == false) {
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("qualitySet", true);
            final DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);

            if (dm.densityDpi > DisplayMetrics.DENSITY_MEDIUM) {
                editor.putBoolean("lowtextures", false);
            } else {
                editor.putBoolean("lowtextures", false);
            }
            editor.commit();
        }

        if (prefs.getBoolean("onlineSet", false) == false) {

            Editor editor = prefs.edit();
            editor.putBoolean("onlineSet", true);
            editor.commit();

            //TODO removed auto registration at first launch
            /*OnlineInitializer initializer = new OnlineInitializer(this);
			initializer.createInitDialog();*/
        }
    }

    public void onLoadResources() {
        Config.setTextureQuality(1);
        ResourceManager.getInstance().Init(mEngine, this);
        ResourceManager.getInstance().loadHighQualityAsset("logo", "logo.png");
        ResourceManager.getInstance().loadHighQualityAsset("play", "play.png");
        //ResourceManager.getInstance().loadHighQualityAsset("multiplayer", "multiplayer.png");
        //ResourceManager.getInstance().loadHighQualityAsset("solo", "solo.png");
        ResourceManager.getInstance().loadHighQualityAsset("exit", "exit.png");
        ResourceManager.getInstance().loadHighQualityAsset("options", "options.png");
        ResourceManager.getInstance().loadHighQualityAsset("star", "gfx/star.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_play", "music_play.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_pause", "music_pause.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_stop", "music_stop.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_next", "music_next.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_prev", "music_prev.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_np", "music_np.png");
        ResourceManager.getInstance().loadHighQualityAsset("songselect-top", "songselect-top.png");
        File bg;
        if ((bg = new File(Config.getSkinPath() + "menu-background.png")).exists()
                || (bg = new File(Config.getSkinPath() + "menu-background.jpg")).exists()) {
            ResourceManager.getInstance().loadHighQualityFile("menu-background", bg);
        }
        // ResourceManager.getInstance().loadHighQualityAsset("exit", "exit.png");
        ResourceManager.getInstance().loadFont("font", null, 28, Color.WHITE);
        ResourceManager.getInstance().loadFont("smallFont", null, 21, Color.WHITE);
        ResourceManager.getInstance().loadStrokeFont("strokeFont", null, 36, Color.BLACK, Color.WHITE);

        BassAudioPlayer.initDevice();

    }

    public Scene onLoadScene() {
        GlobalManager.getInstance().getEngine().registerUpdateHandler(new FPSLogger(1f));
        return new SplashScene().getScene();
    }

    public void onLoadComplete() {
        new AsyncTaskLoader().execute(new OsuAsyncCallback() {
            public void run() {
                /*
                 * StartingMapManager mgr = new
                 * StartingMapManager(OsuActivity.this); mgr.copyStartingMaps();
                 */
                GlobalManager.getInstance().init();
                GlobalManager.getInstance().setLoadingProgress(50);
                checkNewBeatmaps();
                if (!LibraryManager.getInstance().loadLibraryCache(MainActivity.this, true)) {
                    LibraryManager.getInstance().scanLibrary(MainActivity.this);
                    System.gc();
                }
            }

            public void onComplete() {
                GlobalManager.getInstance().setInfo("");
                GlobalManager.getInstance().setLoadingProgress(100);
                ResourceManager.getInstance().loadFont("font", null, 28, Color.WHITE);
                GlobalManager.getInstance().getEngine().setScene(GlobalManager.getInstance().getMainScene().getScene());
                GlobalManager.getInstance().getMainScene().loadBeatmap();
                initPreferences();
                if (willReplay) {
                    GlobalManager.getInstance().getMainScene().watchReplay(beatmapToAdd);
                    willReplay = false;
                }
            }
        });
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onSetContentView() {
        this.mRenderSurfaceView = new RenderSurfaceView(this);
        this.mRenderSurfaceView.setEGLConfigChooser(true);
        this.mRenderSurfaceView.setRenderer(this.mEngine);

        RelativeLayout layout = new RelativeLayout(this);
        layout.setBackgroundColor(Color.argb(255, 0, 0, 0));
        layout.addView(
                mRenderSurfaceView,
                new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT){{
                    addRule(RelativeLayout.CENTER_IN_PARENT);
                }});

        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setId(0x28371);
        layout.addView(frameLayout, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View c = new View(this);
        c.setBackgroundColor(Color.argb(0, 0, 0, 0));
        layout.addView(c, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        this.setContentView(
                layout,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) {{
                    gravity = Gravity.CENTER;
                }});

        ActivityOverlay.initial(this, frameLayout.getId());
    }

    public void checkNewBeatmaps() {
        GlobalManager.getInstance().setInfo("Checking new maps...");
        final File mainDir = new File(Config.getCorePath());
        if (beatmapToAdd != null) {
            File file = new File(beatmapToAdd);
            if (file.getName().endsWith(".osz")) {
                ToastLogger.showText(
                        StringTable.get(R.string.message_lib_importing),
                        false);
                if (OSZParser.parseOSZ(MainActivity.this, beatmapToAdd)) {
                    String folderName = beatmapToAdd.substring(0, beatmapToAdd.length() - 4);
                    // We have imported the beatmap!
                    ToastLogger.showText(
                            StringTable.format(R.string.message_lib_imported, folderName),
                            true);
                }
                LibraryManager.getInstance().sort();
                LibraryManager.getInstance().savetoCache(MainActivity.this);
            } else if (file.getName().endsWith(".odr")) {
                willReplay = true;
            }
        } else if (mainDir.exists() && mainDir.isDirectory()) {
            File[] filelist = mainDir.listFiles();
            final ArrayList<String> beatmaps = new ArrayList<String>();
            for (final File file : filelist) {
                if (file.isFile()
                        && file.getName().endsWith(".osz")
                        || file.getName().endsWith(".zip")) {
                    beatmaps.add(file.getPath());
                }
            }

            File beatmapDir = new File(Config.getBeatmapPath());
            if (beatmapDir.exists()
                    && beatmapDir.isDirectory()) {
                filelist = beatmapDir.listFiles();
                for (final File file : filelist) {
                    if (file.isFile()
                            && file.getName().endsWith(".osz")
                            || file.getName().endsWith(".zip")) {
                        beatmaps.add(file.getPath());
                    }
                }
            }

            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (Config.isSCAN_DOWNLOAD()
                    && downloadDir.exists()
                    && downloadDir.isDirectory()) {
                filelist = downloadDir.listFiles();
                for (final File file : filelist) {
                    if (file.isFile()
                            && file.getName().endsWith(".osz")) {
                        beatmaps.add(file.getPath());
                    }
                }
            }

            if (beatmaps.size() > 0) {
                final boolean deleteOsz = Config.isDELETE_OSZ();
                Config.setDELETE_OSZ(true);
                ToastLogger.showText(StringTable.format(
                        R.string.message_lib_importing_several,
                        beatmaps.size()), false);
                for (final String s : beatmaps) {
                    if (OSZParser.parseOSZ(MainActivity.this, s)) {
                        String folderName = s.substring(0, s.length() - 4);
                        // We have imported the beatmap!
                        ToastLogger.showText(
                                StringTable.format(R.string.message_lib_imported, folderName),
                                true);
                    }
                }
                Config.setDELETE_OSZ(deleteOsz);

                LibraryManager.getInstance().sort();
                LibraryManager.getInstance().savetoCache(
                        MainActivity.this);
            }
        }
    }

    @Override
    protected void onCreate(Bundle pSavedInstanceState) {
        super.onCreate(pSavedInstanceState);
        if (BuildConfig.DEBUG) {
            //Toast.makeText(this,"this is debug version",Toast.LENGTH_LONG).show();
            try {
                File d = new File(Environment.getExternalStorageDirectory(), "osu!droid/Log");
                if (!d.exists()) d.mkdirs();
                File f = new File(d, "rawlog.txt");
                if (!f.exists()) f.createNewFile();
                Runtime.getRuntime().exec("logcat -f " + (f.getAbsolutePath()));
            } catch (IOException e) {
            }
        }
        if (Config.isHideNaviBar()) {
            if (Build.VERSION.SDK_INT >= 11) {
                // BEGIN_INCLUDE (get_current_ui_flags)
                // The UI options currently enabled are represented by a bitfield.
                // getSystemUiVisibility() gives us that bitfield.
                int uiOptions = this.getWindow().getDecorView().getSystemUiVisibility();
                int newUiOptions = uiOptions;
                // END_INCLUDE (get_current_ui_flags)
                // BEGIN_INCLUDE (toggle_ui_flags)
                boolean isImmersiveModeEnabled =
                        ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);

                // Navigation bar hiding:  Backwards compatible to ICS.
                if (Build.VERSION.SDK_INT >= 14) {
                    newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                }

                // Status bar hiding: Backwards compatible to Jellybean
                if (Build.VERSION.SDK_INT >= 16) {
                    newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
                }

                // Immersive mode: Backward compatible to KitKat.
                // Note that this flag doesn't do anything by itself, it only augments the behavior
                // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
                // all three flags are being toggled together.
                // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
                // Sticky immersive mode differs in that it makes the navigation and status bars
                // semi-transparent, and the UI flag does not get cleared when the user interacts with
                // the screen.
                if (Build.VERSION.SDK_INT >= 18) {
                    newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                }

                this.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
                //END_INCLUDE (set_ui_flags)
            }
        }
        onBeginBindService();
    }

    public void onCreateNotifyReceiver() {
        if (filter == null) {
            //过滤器创建
            filter = new IntentFilter();
            filter.addAction("Notify_cancel");
        }

        //按钮广播监听
        onNotifyButtonClick = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("Notify_cancel")) {
                    songService.stop();
                    GlobalManager.getInstance().getMainScene().exit();
                }
            }
        };
        registerReceiver(onNotifyButtonClick, filter);
    }

    public void onBeginBindService() {
        if (connection == null && songService == null) {
            connection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    songService = ((SongService.ReturnBindObject) service).getObject();
                    saveServiceObject = (SaveServiceObject) getApplication();
                    saveServiceObject.setSongService(songService);
                    GlobalManager.getInstance().setSongService(songService);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }

            };

            bindService(new Intent(MainActivity.this, SongService.class), connection, BIND_AUTO_CREATE);
            if (Build.VERSION.SDK_INT > 10) {
                onCreateNotifyReceiver();
            }
        }
        GlobalManager.getInstance().setSongService(songService);
        GlobalManager.getInstance().setSaveServiceObject(saveServiceObject);
    }

    @Override
    protected void onStart() {
//		this.enableAccelerometerSensor(this);
        if (getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            if (ContentResolver.SCHEME_FILE.equals(getIntent().getData().getScheme())) {
                beatmapToAdd = getIntent().getData().getPath();
            }
            if (BuildConfig.DEBUG) {
                System.out.println(getIntent());
                System.out.println(getIntent().getData().getEncodedPath());
            }
        }
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (GlobalManager.getInstance().getSkinNow() != null) {
            if (GlobalManager.getInstance().getSkinNow() != Config.getSkinPath()) {
                GlobalManager.getInstance().setSkinNow(Config.getSkinPath());
                ToastLogger.showText(StringTable.get(R.string.message_loading_skin), true);
                ResourceManager.getInstance().loadCustomSkin(Config.getSkinPath());
            }
        }
        if (GlobalManager.getInstance().getEngine() != null && GlobalManager.getInstance().getGameScene() != null
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getGameScene().getScene()) {
            GlobalManager.getInstance().getEngine().getTextureManager().reloadTextures();
        }
        if (GlobalManager.getInstance().getMainScene() != null) {
            if (songService != null && Build.VERSION.SDK_INT > 10) {
                if (songService.hideNotifyPanel()) {
                    if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
                    GlobalManager.getInstance().getMainScene().loadBeatmapInfo();
                    GlobalManager.getInstance().getMainScene().loadTimeingPoints(false);
                    GlobalManager.getInstance().getMainScene().progressBar.setTime(songService.getLength());
                    GlobalManager.getInstance().getMainScene().progressBar.setPassedTime(songService.getPosition());
                    GlobalManager.getInstance().getMainScene().musicControl(MainScene.MusicOption.SYNC);
                }
            }
        }
        MobclickAgent.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (GlobalManager.getInstance().getEngine() != null && GlobalManager.getInstance().getGameScene() != null
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getGameScene().getScene()) {
            SpritePool.getInstance().purge();
            GlobalManager.getInstance().getGameScene().pause();
        }
        if (GlobalManager.getInstance().getMainScene() != null) {
            BeatmapInfo beatmapInfo = GlobalManager.getInstance().getMainScene().beatmapInfo;
            if (songService != null && beatmapInfo != null && !songService.isGaming() && !songService.isSettingMenu()) {
                if (Build.VERSION.SDK_INT > 10) {
                    songService.showNotifyPanel();

                    if (wakeLock == null) {
                        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "osudroid:MainActivity");
                    }
                    wakeLock.acquire();

                    if (beatmapInfo.getArtistUnicode() != null && beatmapInfo.getTitleUnicode() != null) {
                        songService.updateTitleText(beatmapInfo.getTitleUnicode(), beatmapInfo.getArtistUnicode());
                    } else if (beatmapInfo.getArtist() != null && beatmapInfo.getTitle() != null) {
                        songService.updateTitleText(beatmapInfo.getTitle(), beatmapInfo.getArtist());
                    } else {
                        songService.updateTitleText("QAQ I cant load info", " ");
                    }
                    songService.updateCoverImage(beatmapInfo.getTrack(0).getBackground());
                    songService.updateStatus();
                } else {
                    songService.stop();
                }
            } else {
                if (songService != null) {
                    songService.pause();
                }
            }
        }
        MobclickAgent.onPause(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (GlobalManager.getInstance().getEngine() != null
                && GlobalManager.getInstance().getGameScene() != null
                && !hasFocus
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getGameScene().getScene()) {
            if (!GlobalManager.getInstance().getGameScene().isPaused()) {
                GlobalManager.getInstance().getGameScene().pause();
            }
        }
        if (hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        	getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
    }

//    @Override
//    public void onBackPressed() {
//        MainManager.getInstance().getMainScene().exit();
//    }

    @Override
    public void onAccelerometerChanged(final AccelerometerData arg0) {
        if (GlobalManager.getInstance().getCamera().getRotation() == 0 && arg0.getY() < -5) {
            GlobalManager.getInstance().getCamera().setRotation(180);
        } else if (GlobalManager.getInstance().getCamera().getRotation() == 180 && arg0.getY() > 5) {
            GlobalManager.getInstance().getCamera().setRotation(0);
        }
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        /*boolean superDown = super.onKeyDown(keyCode, event);
        if (superDown) {
            return superDown;
        }*/

        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.onKeyDown(keyCode, event);
        }
        if (GlobalManager.getInstance().getEngine() == null) {
            return super.onKeyDown(keyCode, event);
        }

        if (event.getAction() == TouchEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK && ActivityOverlay.onBackPress()) {
            return true;
        }


        if (GlobalManager.getInstance().getGameScene() != null
                && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU)
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getGameScene().getScene()) {
            if (GlobalManager.getInstance().getGameScene().isPaused()) {
                GlobalManager.getInstance().getGameScene().resume();
            } else {
                GlobalManager.getInstance().getGameScene().pause();
            }
            return true;
        }
        if (GlobalManager.getInstance().getScoring() != null && keyCode == KeyEvent.KEYCODE_BACK
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getScoring().getScene()) {
            GlobalManager.getInstance().getScoring().replayMusic();
            GlobalManager.getInstance().getEngine().setScene(GlobalManager.getInstance().getSongMenu().getScene());
            GlobalManager.getInstance().getSongMenu().updateScore();
            ResourceManager.getInstance().getSound("applause").stop();
            GlobalManager.getInstance().getScoring().setReplayID(-1);
            return true;
        }
        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ENTER)
                && GlobalManager.getInstance().getEngine() != null
                && GlobalManager.getInstance().getSongMenu() != null
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getSongMenu().getScene()
                && GlobalManager.getInstance().getSongMenu().getScene().hasChildScene()) {
            if (FilterMenu.getInstance().getClass() == FilterMenu.class) {
                if (GlobalManager.getInstance().getSongMenu().getScene().getChildScene() == FilterMenu.getInstance()
                        .getScene()) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        InputManager.getInstance().toggleKeyboard();
                    }
                    FilterMenu.getInstance().hideMenu();
                }
            }

            /*if (GlobalManager.getInstance().getSongMenu().getScene().getChildScene() == PropsMenu.getInstance()
                    .getScene()) {
                PropsMenu.getInstance().saveChanges();
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    InputManager.getInstance().toggleKeyboard();
                }
            }*/

            if (GlobalManager.getInstance().getSongMenu().getScene().getChildScene() == ModMenu.getInstance().getScene()) {
                ModMenu.getInstance().hide();
            }

            return true;
        }
        if (GlobalManager.getInstance().getSongMenu() != null && GlobalManager.getInstance().getEngine() != null
                && keyCode == KeyEvent.KEYCODE_MENU
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getSongMenu().getScene()
                && GlobalManager.getInstance().getSongMenu().getScene().hasChildScene() == false) {
            GlobalManager.getInstance().getSongMenu().stopScroll(0);
            GlobalManager.getInstance().getSongMenu().showPropertiesMenu(null);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (GlobalManager.getInstance().getEngine() != null && GlobalManager.getInstance().getSongMenu() != null &&
                    GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getSongMenu().getScene()) {

                //SongMenu 界面按返回按钮（系统按钮）
                GlobalManager.getInstance().getSongMenu().back();
            } else {

                if (GlobalManager.getInstance().getEngine().getScene() instanceof LoadingScreen.LoadingScene) {
                    return true;
                }

                GlobalManager.getInstance().getMainScene().exit();
                if (wakeLock != null && wakeLock.isHeld()) {
                    wakeLock.release();
                }
            }
            return true;
        }

        if (InputManager.getInstance().isStarted()) {
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                InputManager.getInstance().pop();
            } else if (keyCode != KeyEvent.KEYCODE_ENTER) {
                final char c = (char) event.getUnicodeChar();
                if (c != 0) {
                    InputManager.getInstance().append(c);
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Need permission to access osu!droid directory");
                builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission
                                .WRITE_EXTERNAL_STORAGE}, 2333);
                    }
                });
                builder.show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2333);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 2333: {
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
                            if (!showRationale) {// user denied flagging NEVER ASK AGAIN
                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setMessage("Game won't run without storage permission!\nPlease enable it manually.");
                                builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivityForResult(intent, 2334);
                                    }
                                });
                                builder.setNegativeButton(R.string.dialog_cancel, null);
                                builder.show();

                            } else {
                                // user denied WITHOUT never ask again
                                // game will exit by exception
                            }
                        }
                    }
                }
            }
        }
    }
}
