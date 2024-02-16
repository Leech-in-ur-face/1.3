package com.reco1l.legacy.ui.multiplayer

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnKeyListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import org.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osuplus.R
import kotlin.math.abs

class LobbySearch : BaseFragment(), OnEditorActionListener, OnKeyListener
{
    override val layoutID = R.layout.multiplayer_lobby_search

    var field: EditText? = null

    private val isExtended: Boolean
        get() = findViewById<View?>(R.id.fullLayout) != null && abs(findViewById<View>(R.id.fullLayout)!!.translationY) < 10

    init
    {
        isDismissOnBackPress = false
    }

    override fun onLoadView()
    {
        reload()

        field = findViewById(R.id.search_field)!!
        field!!.setOnEditorActionListener(this)
        field!!.setOnKeyListener(this)

        findViewById<View>(R.id.frg_header)!!.animate().cancel()
        findViewById<View>(R.id.frg_header)!!.alpha = 0f
        findViewById<View>(R.id.frg_header)!!.translationY = 100f
        findViewById<View>(R.id.frg_header)!!.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                .start()
    }

    private fun hideKeyboard()
    {
        field?.clearFocus()

        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(field?.windowToken, 0)
    }

    override fun onEditorAction(view: TextView?, actionId: Int, event: KeyEvent?): Boolean
    {
        if (actionId == EditorInfo.IME_ACTION_SEND)
        {
            hideKeyboard()
            LobbyScene.searchQuery = view?.text?.toString()
            return true
        }
        return false
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean
    {
        if (keyCode == KeyEvent.KEYCODE_ENTER && v is EditText)
        {
            onEditorAction(v, EditorInfo.IME_ACTION_SEND, event)
            return true
        }
        return false
    }

    private fun reload()
    {
        val showMoreButton = findViewById<View>(R.id.showMoreButton) ?: return
        showMoreButton.setOnTouchListener { v: View, event: MotionEvent ->
            if (event.action == TouchEvent.ACTION_DOWN)
            {
                v.animate().cancel()
                v.animate().scaleY(0.9f).scaleX(0.9f).translationY(v.height * 0.1f).setDuration(100).start()
                toggleVisibility()
                return@setOnTouchListener true
            }
            else if (event.action == TouchEvent.ACTION_UP)
            {
                v.animate().cancel()
                v.animate().scaleY(1f).scaleX(1f).setDuration(100).translationY(0f).start()
                return@setOnTouchListener true
            }
            false
        }
        findViewById<View>(R.id.frg_background)!!.isClickable = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun toggleVisibility()
    {
        field?.clearFocus()

        if (isExtended)
        {
            playHidePanelAnim()
            findViewById<View>(R.id.frg_background)!!.setOnTouchListener(null)
            findViewById<View>(R.id.frg_background)!!.isClickable = false
        }
        else
        {
            playShowPanelAnim()
            findViewById<View>(R.id.frg_background)!!.setOnTouchListener { _, event ->
                if (event.action == TouchEvent.ACTION_DOWN)
                {
                    if (isExtended)
                    {
                        toggleVisibility()
                        return@setOnTouchListener true
                    }
                }
                false
            }
            findViewById<View>(R.id.frg_background)!!.isClickable = true
        }
    }

    private fun playShowPanelAnim()
    {
        val fullLayout = findViewById<View>(R.id.fullLayout)
        if (fullLayout != null)
        {
            fullLayout.animate().cancel()
            fullLayout.animate()
                    .translationY(0f)
                    .setDuration(200)
                    .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                    .setListener(object : BaseAnimationListener()
                                 {
                                     override fun onAnimationEnd(animation: Animator)
                                     {
                                         super.onAnimationEnd(animation)
                                         findViewById<View>(R.id.frg_background)!!.isClickable = true
                                         findViewById<View>(R.id.frg_background)!!.setOnClickListener { playHidePanelAnim() }
                                     }
                                 })
                    .start()
        }
    }

    private fun playHidePanelAnim()
    {
        val fullLayout = findViewById<View>(R.id.fullLayout)
        if (fullLayout != null)
        {
            fullLayout.animate().cancel()
            fullLayout.animate()
                    .translationY(findViewById<View>(R.id.optionBody)!!.height.toFloat())
                    .setDuration(200)
                    .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                    .setListener(object : BaseAnimationListener()
                                 {
                                     override fun onAnimationEnd(animation: Animator)
                                     {
                                         super.onAnimationEnd(animation)
                                         findViewById<View>(R.id.frg_background)!!.isClickable = false
                                     }
                                 })
                    .start()
        }
    }

    override fun callDismissOnBackPress()
    {
        if (isExtended)
            toggleVisibility()

        dismiss()
        LobbyScene.back()
    }
}
