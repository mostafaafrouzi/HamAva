/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.ui.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.afrouzi.hamava.MainActivity
import com.afrouzi.hamava.R
import com.afrouzi.hamava.data.model.AudioSourceType
import com.afrouzi.hamava.data.model.DubSettings
import com.afrouzi.hamava.data.model.DubStatus
import kotlin.math.abs

class FloatingOverlayManager(
    private val context: Context,
    private val onTogglePause: () -> Unit,
    private val onStopSession: () -> Unit,
    private val onOriginalVolumeChanged: (Float) -> Unit
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayRootView: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // UI Elements
    private var bubbleContainer: FrameLayout? = null
    private var bubbleGlowView: View? = null
    private var bubbleIcon: ImageView? = null
    private var panelContainer: LinearLayout? = null
    private var statusBadge: TextView? = null
    private var latencyBadge: TextView? = null
    private var subtitleTicker: TextView? = null
    private var playPauseButton: TextView? = null
    private var stopButton: TextView? = null
    private var volumeSlider: SeekBar? = null
    private var volumePercentText: TextView? = null

    private var currentStatus: DubStatus = DubStatus.IDLE
    private var isExpanded = false
    private var isOverlayVisible = false

    private var screenWidth = 1080
    private var screenHeight = 1920

    // Touch & Drag state
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    // iOS Dark Palette
    private val iosCardBg = Color.parseColor("#1C1C1E")
    private val iosControlBg = Color.parseColor("#2C2C2E")
    private val iosSubtleBg = Color.parseColor("#141416")
    private val iosBorderColor = Color.parseColor("#38383A")
    private val iosBlue = Color.parseColor("#0A84FF")
    private val iosGreen = Color.parseColor("#34C759")
    private val iosOrange = Color.parseColor("#FF9F0A")
    private val iosRed = Color.parseColor("#FF453A")
    private val tealColor = Color.parseColor("#00D4AA")
    private val purpleColor = Color.parseColor("#6750A4")

    init {
        updateScreenDimensions()
    }

    private fun updateScreenDimensions() {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(dm)
        screenWidth = dm.widthPixels
        screenHeight = dm.heightPixels
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }

    private fun createRoundedDrawable(bgColor: Int, cornerRadiusPx: Float, strokeColor: Int? = null, strokeWidthPx: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusPx
            setColor(bgColor)
            if (strokeColor != null && strokeWidthPx > 0) {
                setStroke(strokeWidthPx, strokeColor)
            }
        }
    }

    fun isPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show(settings: DubSettings) {
        if (!isPermissionGranted() || isOverlayVisible) return

        try {
            updateScreenDimensions()

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = screenWidth - dpToPx(76f)
                y = screenHeight / 3
            }

            buildViewHierarchy(settings)

            overlayRootView?.let { root ->
                windowManager.addView(root, layoutParams)
                isOverlayVisible = true
                Log.d("HamAva", "FloatingOverlayManager: Overlay attached successfully")
            }
        } catch (e: Exception) {
            Log.e("HamAva", "Failed to show floating overlay: ${e.localizedMessage}", e)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildViewHierarchy(settings: DubSettings) {
        val root = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // --- 1. Minimized Bubble ---
        val bubbleSize = dpToPx(56f)
        val bubble = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
        }

        // Outer glow
        bubbleGlowView = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(purpleColor)
                setStroke(dpToPx(2.5f), tealColor)
            }
        }
        bubble.addView(bubbleGlowView)

        // Center mic / play icon
        bubbleIcon = ImageView(context).apply {
            val iconPad = dpToPx(13f)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(iconPad, iconPad, iconPad, iconPad)
            setImageResource(R.drawable.ic_tile_mic)
            setColorFilter(Color.WHITE)
        }
        bubble.addView(bubbleIcon)

        // Touch listener for dragging & click
        bubble.setOnTouchListener { _, event ->
            handleBubbleTouch(event)
        }

        bubbleContainer = bubble
        root.addView(bubble)

        // --- 2. Expanded Control Panel (iOS Style) ---
        val panelWidth = dpToPx(270f)
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            visibility = View.GONE
            val pad = dpToPx(16f)
            setPadding(pad, pad, pad, pad)
            layoutParams = FrameLayout.LayoutParams(panelWidth, FrameLayout.LayoutParams.WRAP_CONTENT)
            background = createRoundedDrawable(
                bgColor = iosCardBg,
                cornerRadiusPx = dpToPx(22f).toFloat(),
                strokeColor = iosBorderColor,
                strokeWidthPx = dpToPx(1.5f)
            )
        }

        // Header: Dynamic Island Style Capsule + Close Button
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleCapsule = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createRoundedDrawable(iosControlBg, dpToPx(12f).toFloat())
            val px = dpToPx(10f)
            val py = dpToPx(4f)
            setPadding(px, py, px, py)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val brandDot = View(context).apply {
            val s = dpToPx(8f)
            layoutParams = LinearLayout.LayoutParams(s, s).apply {
                leftMargin = dpToPx(6f)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(tealColor)
            }
        }
        titleCapsule.addView(brandDot)

        val titleText = TextView(context).apply {
            text = "هم‌آوا • دوبله زنده"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        }
        titleCapsule.addView(titleText)
        headerRow.addView(titleCapsule)

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#8E8E93"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            val hitSize = dpToPx(28f)
            layoutParams = LinearLayout.LayoutParams(hitSize, hitSize).apply {
                rightMargin = dpToPx(6f)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(iosControlBg)
            }
            setOnClickListener { collapseToBubble() }
        }
        headerRow.addView(closeBtn)
        panel.addView(headerRow)

        // Status Row (Status Text + Latency pill)
        val statusRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val mTop = dpToPx(10f)
            setPadding(0, mTop, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        statusBadge = TextView(context).apply {
            text = "● آماده"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        statusRow.addView(statusBadge)

        latencyBadge = TextView(context).apply {
            text = "-- ms"
            setTextColor(tealColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            val p = dpToPx(4f)
            setPadding(dpToPx(8f), p, dpToPx(8f), p)
            background = createRoundedDrawable(iosSubtleBg, dpToPx(8f).toFloat(), iosBorderColor, dpToPx(1f))
        }
        statusRow.addView(latencyBadge)
        panel.addView(statusRow)

        // Source & Target Language indicator
        val langRow = TextView(context).apply {
            val sourceName = if (settings.audioSource == AudioSourceType.MIC) "میکروفون" else "صدای سیستم"
            text = "$sourceName ➔ ${settings.targetLanguage.nameFa}"
            setTextColor(Color.parseColor("#98989F"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            val mTop = dpToPx(4f)
            setPadding(0, mTop, 0, 0)
        }
        panel.addView(langRow)

        // Subtitle Ticker (if enabled)
        subtitleTicker = TextView(context).apply {
            text = "در انتظار گفتار..."
            setTextColor(Color.parseColor("#F2F2F7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            val mTop = dpToPx(8f)
            setPadding(dpToPx(10f), dpToPx(6f), dpToPx(10f), dpToPx(6f))
            background = createRoundedDrawable(iosSubtleBg, dpToPx(10f).toFloat(), iosBorderColor, dpToPx(1f))
            visibility = if (settings.enableSubtitles) View.VISIBLE else View.GONE
        }
        panel.addView(subtitleTicker)

        // Original Volume row (صدای ویدیوی پس‌زمینه)
        val volHeaderRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val mTop = dpToPx(10f)
            setPadding(0, mTop, 0, 0)
        }
        val volLabel = TextView(context).apply {
            text = "بلندی صدای ویدیو (اصلی):"
            setTextColor(Color.parseColor("#E5E5EA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        volHeaderRow.addView(volLabel)

        volumePercentText = TextView(context).apply {
            text = "${(settings.originalAudioVolume * 100).toInt()}%"
            setTextColor(iosBlue)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        volHeaderRow.addView(volumePercentText)
        panel.addView(volHeaderRow)

        volumeSlider = SeekBar(context).apply {
            max = 100
            progress = (settings.originalAudioVolume * 100).toInt().coerceIn(0, 100)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val ratio = progress / 100f
                        volumePercentText?.text = "$progress%"
                        onOriginalVolumeChanged(ratio)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        panel.addView(volumeSlider)

        // Dual Action Controls: Pause/Resume Toggle + Full Stop
        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val mTop = dpToPx(12f)
            setPadding(0, mTop, 0, 0)
        }

        playPauseButton = TextView(context).apply {
            text = "⏸ توقف موقت"
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            val p = dpToPx(8f)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
            background = createRoundedDrawable(iosOrange, dpToPx(12f).toFloat())
            setOnClickListener {
                onTogglePause()
            }
        }
        actionRow.addView(playPauseButton)

        val spacer1 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(6f), 1)
        }
        actionRow.addView(spacer1)

        stopButton = TextView(context).apply {
            text = "✕ خروج"
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            val p = dpToPx(8f)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
            background = createRoundedDrawable(iosRed, dpToPx(12f).toFloat())
            setOnClickListener {
                onStopSession()
            }
        }
        actionRow.addView(stopButton)

        val spacer2 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(6f), 1)
        }
        actionRow.addView(spacer2)

        val openAppBtn = TextView(context).apply {
            text = "برنامه"
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            val p = dpToPx(8f)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
            background = createRoundedDrawable(iosControlBg, dpToPx(12f).toFloat(), iosBorderColor, dpToPx(1f))
            setOnClickListener {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                context.startActivity(intent)
                collapseToBubble()
            }
        }
        actionRow.addView(openAppBtn)
        panel.addView(actionRow)

        panelContainer = panel
        root.addView(panel)

        overlayRootView = root
    }

    private fun handleBubbleTouch(event: MotionEvent): Boolean {
        val params = layoutParams ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()
                if (abs(dx) > 10 || abs(dy) > 10) {
                    isDragging = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(overlayRootView, params)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    // Tap clicked!
                    expandToPanel()
                } else {
                    // Snap to closest screen edge (Left or Right)
                    snapToEdge()
                }
                return true
            }
        }
        return false
    }

    private fun snapToEdge() {
        val params = layoutParams ?: return
        val currentX = params.x
        val targetX = if (currentX + dpToPx(28f) < screenWidth / 2) dpToPx(8f) else screenWidth - dpToPx(64f)

        val animator = ValueAnimator.ofInt(currentX, targetX).apply {
            duration = 200
            interpolator = OvershootInterpolator(0.8f)
            addUpdateListener { va ->
                params.x = va.animatedValue as Int
                windowManager.updateViewLayout(overlayRootView, params)
            }
        }
        animator.start()
    }

    private fun expandToPanel() {
        if (isExpanded) return
        isExpanded = true
        bubbleContainer?.visibility = View.GONE
        panelContainer?.visibility = View.VISIBLE

        layoutParams?.let { params ->
            params.x = (screenWidth - dpToPx(270f)).coerceAtLeast(dpToPx(16f)) / 2
            windowManager.updateViewLayout(overlayRootView, params)
        }
    }

    private fun collapseToBubble() {
        if (!isExpanded) return
        isExpanded = false
        panelContainer?.visibility = View.GONE
        bubbleContainer?.visibility = View.VISIBLE

        layoutParams?.let { params ->
            params.x = screenWidth - dpToPx(64f)
            windowManager.updateViewLayout(overlayRootView, params)
        }
    }

    fun updateStatus(status: DubStatus) {
        currentStatus = status
        mainHandler.post {
            val (text, color) = when (status) {
                DubStatus.IDLE -> "● آماده" to Color.LTGRAY
                DubStatus.CONNECTING -> "● در حال اتصال..." to tealColor
                DubStatus.ACTIVE_LISTENING -> "● در حال شنیدن..." to tealColor
                DubStatus.ACTIVE_SPEAKING -> "● در حال دوبله..." to iosGreen
                DubStatus.ERROR -> "● خطا در اتصال" to iosRed
                DubStatus.PAUSED -> "⏸ موقتاً متوقف" to iosOrange
            }

            statusBadge?.text = text
            statusBadge?.setTextColor(color)

            if (status == DubStatus.PAUSED) {
                playPauseButton?.text = "▶ ادامه دوبله"
                playPauseButton?.background = createRoundedDrawable(iosGreen, dpToPx(12f).toFloat())
                bubbleIcon?.setImageResource(R.drawable.ic_play_arrow)
                (bubbleGlowView?.background as? GradientDrawable)?.apply {
                    setColor(Color.parseColor("#3A2E1C"))
                    setStroke(dpToPx(2.5f), iosOrange)
                }
            } else {
                playPauseButton?.text = "⏸ توقف موقت"
                playPauseButton?.background = createRoundedDrawable(iosOrange, dpToPx(12f).toFloat())
                bubbleIcon?.setImageResource(R.drawable.ic_tile_mic)
                (bubbleGlowView?.background as? GradientDrawable)?.apply {
                    setColor(purpleColor)
                    val strokeColor = if (status == DubStatus.ACTIVE_SPEAKING) iosGreen else tealColor
                    setStroke(dpToPx(2.5f), strokeColor)
                }
            }
        }
    }

    fun updateLatency(latencyMs: Long) {
        mainHandler.post {
            latencyBadge?.text = "$latencyMs ms"
        }
    }

    fun updateSubtitle(text: String) {
        if (text.isNotBlank()) {
            mainHandler.post {
                subtitleTicker?.text = text
            }
        }
    }

    fun hide() {
        mainHandler.post {
            if (!isOverlayVisible || overlayRootView == null) return@post
            try {
                windowManager.removeView(overlayRootView)
                isOverlayVisible = false
                overlayRootView = null
                Log.d("HamAva", "FloatingOverlayManager: Overlay removed cleanly")
            } catch (e: Exception) {
                Log.w("HamAva", "Error removing floating overlay: ${e.localizedMessage}")
            }
        }
    }

    fun isVisible(): Boolean = isOverlayVisible
}
