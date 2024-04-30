package com.reco1l.legacy.ui

import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.edlplan.ui.fragment.LoadingFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.reco1l.framework.net.Downloader
import ru.nsu.ccfit.zuev.osuplus.R

class DownloadFragment : LoadingFragment() {

    override val layoutID = R.layout.fragment_downloading


    private lateinit var downloader: Downloader

    private lateinit var onLoad: Runnable


    lateinit var button: Button
        private set

    lateinit var text: TextView
        private set

    lateinit var progressBar: CircularProgressIndicator
        private set


    override fun onLoadView() {
        super.onLoadView()

        text = findViewById(R.id.text)!!
        button = findViewById(R.id.button)!!
        progressBar = findViewById(R.id.progress)!!

        onLoad.run()
    }


    fun setDownloader(downloader: Downloader, onLoad: Runnable) {
        this.onLoad = onLoad
        this.downloader = downloader
    }

    fun setText(content: String) {
        text.visibility = if (TextUtils.isEmpty(content)) View.GONE else View.VISIBLE
        text.text = content
    }


    override fun callDismissOnBackPress() {
        if (downloader.isDownloading) {
            downloader.cancel()
            return
        }

        super.callDismissOnBackPress()
    }
}