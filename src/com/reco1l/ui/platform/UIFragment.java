package com.reco1l.ui.platform;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.reco1l.Scenes;
import com.reco1l.utils.listeners.TouchListener;
import com.reco1l.utils.ViewTouchHandler;
import com.reco1l.utils.Resources;
import com.reco1l.interfaces.IMainClasses;

import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/6/22 02:26
// Based on the EdrowsLuo BaseFragment class :)

public abstract class UIFragment extends Fragment implements IMainClasses, UI {

    public boolean isShowing = false;

    protected View rootView, rootBackground;
    protected boolean isDismissOnBackgroundPress = false,
            isDismissOnBackPress = true,
            isLoaded = false;

    protected int screenWidth = Config.getRES_WIDTH();
    protected int screenHeight = Config.getRES_HEIGHT();

    protected final Map<View, ViewTouchHandler> registeredViews;

    private final Runnable close = this::close;

    //--------------------------------------------------------------------------------------------//

    public UIFragment() {
        registeredViews = new HashMap<>();
    }

    //--------------------------------------------------------------------------------------------//
    /**
     * Runs once the layout XML is inflated.
     */
    protected abstract void onLoad();

    /**
     * Simplifies the way views are got with the method {@link #find(String)}, every layout XML file have an
     * undefined prefix (you have to define it on every view ID declaration).
     */
    protected abstract String getPrefix();
    protected abstract @LayoutRes int getLayout();

    /**
     * Defines which scene the fragment belongs to.
     * <p>Note: If you set it to <code>null</code> it will gonna be added to the main container
     * (use this only in extras or dialogs)</p>
     */
    protected Scenes getParentScene() { return null; }

    /**
     * Sets the time of inactivity that need to be reached to close the fragment.
     * <p>Note: Use this only on extras dialogs.</p>
     */
    protected long getDismissTime() { return 0; }
    //--------------------------------------------------------------------------------------------//

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {

        rootView = inflater.inflate(getLayout(), container, false);

        // Don't forget to create a View matching root bounds and set its ID to "background" for this feature.
        // You can also set the root view ID as "background".
        rootBackground = find(R.id.background);
        onLoad();
        isLoaded = true;
        if (isDismissOnBackgroundPress && rootBackground != null) {
            rootBackground.setClickable(true);

            if (!rootBackground.hasOnClickListeners()) {
                bindTouchListener(rootBackground, new TouchListener() {
                    public boolean hasTouchEffect() { return false; }
                    public boolean isOnlyOnce() { return true; }

                    public void onPressUp() {
                        close();
                    }
                });
            }
        }
        if (getDismissTime() > 0) {
            rootView.postDelayed(close, getDismissTime());
        }
        return rootView;
    }

    //---------------------------------------Management-------------------------------------------//

    /**
     * Dismiss the layout.
     * <p>
     * If you override this method always compare if {@linkplain #isShowing} is <code>true</code> at
     * the start of the method, otherwise any action with a View that is not showing will throw a
     * {@link NullPointerException}.
     * <p>
     * Also don't forget to call <code>super.close()</code> otherwise the layout will not dismiss, if you add
     * animations call it at the end of the animation, otherwise the animation will broke up.
     */
    public void close() {
        if (!isShowing)
            return;
        rootView.removeCallbacks(close);
        platform.removeFragment(this);
        isShowing = false;
        isLoaded = false;
        unbindTouchListeners();
        registeredViews.clear();
        System.gc();
    }

    public void show() {
        if (isShowing)
            return;
        isLoaded = false;
        String tag = this.getClass().getName() + "@" + this.hashCode();

        if (getParentScene() != null) {
            platform.addSceneFragment(getParentScene(), this, tag);
        } else {
            platform.addFragment(this, tag);
        }
        isShowing = true;
        System.gc();
    }

    /**
     * If the layout is showing then dismiss it, otherwise shows it.
     */
    public void altShow() {
        if (isShowing) {
            close();
        } else {
            show();
        }
    }

    /**
     * @param onBackgroundPress allows the user dismiss the fragment when the background is pressed.
     *                          <p> default value is: <code>false</code>.
     * <p>
     * @param onBackPress allows the user dismiss the fragment when the back button is pressed.
     *                    <p> default value is: <code>true</code>.
     */
    protected void setDismissMode(boolean onBackgroundPress, boolean onBackPress) {
        isDismissOnBackgroundPress = onBackgroundPress;
        isDismissOnBackPress = onBackPress;
    }

    //--------------------------------------------------------------------------------------------//
    /**
     * Finds a child View of the parent layout from its resource ID.
     * @return the view itself if it exists as child in the layout, otherwise null.
     */
    @SuppressWarnings("unchecked")
    protected <T extends View> T find(@IdRes int id) {
        if (rootView == null || id == 0)
            return null;
        Object object = rootView.findViewById(id);

        return object != null ? (T) object : null;
    }

    /**
     * Finds a child View of the parent layout from its ID name in String format.
     * <p>
     *     Note: if you previously defined the layout prefix with the method {@link #getPrefix()}
     *     you don't need to add the prefix to the ID name.
     * @return the view itself if it exists as child in the layout, otherwise null.
     */
    @SuppressWarnings("unchecked")
    protected <T extends View> T find(String id) {
        if (rootView == null || id == null)
            return null;

        int identifier;
        if (getPrefix() == null || id.startsWith(getPrefix())) {
            identifier = Resources.id(id, "id");
        } else {
            identifier = Resources.id(getPrefix() + "_" + id, "id");
        }

        Object view = rootView.findViewById(identifier);
        if (view != null) {
            registeredViews.put((T) view, null);
        }
        return (T) view;
    }

    /**
     * Simple method to check nullability of multiples views at once.
     */
    protected boolean isNull(View... views) {
        for (View view: views) {
            if (view == null)
                return true;
        }
        return false;
    }

    //--------------------------------------------------------------------------------------------//

    public void onTouchEventNotified(int action) {
        if (getDismissTime() > 0) {
            if (action == MotionEvent.ACTION_DOWN) {
                rootView.removeCallbacks(close);
            }
            if (action == MotionEvent.ACTION_UP) {
                rootView.postDelayed(close, getDismissTime());
            }
        }
    }

    protected void unbindTouchListener(View view) {
        if (view == null)
            return;

        view.setOnTouchListener(null);
        if (registeredViews.containsKey(view)) {
            registeredViews.put(view, null);
        }
    }

    protected void bindTouchListener(View view, Runnable onSingleTapUp) {
        bindTouchListener(view, new TouchListener() {
            public void onPressUp() {
                onSingleTapUp.run();
            }
        });
    }

    protected void bindTouchListener(View view, TouchListener listener) {
        ViewTouchHandler touchHandler = registeredViews.get(view);
        if (touchHandler == null) {
            touchHandler = new ViewTouchHandler(listener);
            registeredViews.put(view, touchHandler);
        } else {
            touchHandler.listener = listener;
        }
        touchHandler.linkToFragment(this);
        touchHandler.apply(view);
    }

    protected void unbindTouchListeners() {
        for (View view : registeredViews.keySet()) {
            view.setOnTouchListener(null);
        }
    }

    protected void rebindTouchListeners() {
        for (View view : registeredViews.keySet()) {
            ViewTouchHandler touchHandler = registeredViews.get(view);
            if (touchHandler != null) {
                touchHandler.apply(view);
            }
        }
    }
}
